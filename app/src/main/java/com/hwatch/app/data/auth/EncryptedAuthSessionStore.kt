package com.hwatch.app.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.authSessionDataStore by preferencesDataStore(name = "auth_session")

/**
 * Android Keystore 支持的 AES/GCM 加密会话存储。仅持久化 hwatch_session 值、CSRF、origin，
 * 不写入日志；不引入大型安全框架。
 */
class EncryptedAuthSessionStore(context: Context) : AuthSessionStore {

    private val dataStore = context.applicationContext.authSessionDataStore
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun load(): StoredAuthSession? {
        val encoded = dataStore.data.first()[KEY] ?: return null
        val bytes = KeystoreCrypto.decrypt(encoded) ?: return null
        return runCatching {
            json.decodeFromString(StoredAuthSession.serializer(), bytes.toString(Charsets.UTF_8))
        }.getOrNull()
    }

    override suspend fun save(session: StoredAuthSession) {
        val plain = json.encodeToString(StoredAuthSession.serializer(), session).toByteArray(Charsets.UTF_8)
        val encoded = KeystoreCrypto.encrypt(plain)
        dataStore.edit { prefs -> prefs[KEY] = encoded }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(KEY) }
    }

    companion object {
        private val KEY = stringPreferencesKey("session")
    }
}

/** Android Keystore AES/GCM。 */
internal object KeystoreCrypto {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "hwatch.auth.session"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    fun encrypt(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain)
        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): ByteArray? = runCatching {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        if (bytes.size <= IV_BYTES) return null
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cipherText = bytes.copyOfRange(IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.doFinal(cipherText)
    }.getOrNull()
}
