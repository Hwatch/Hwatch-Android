package com.hwatch.app.testutil

import com.hwatch.app.data.auth.AuthSessionStore
import com.hwatch.app.data.auth.StoredAuthSession

/** JVM 内存会话存储（生产用 Keystore 加密实现）。 */
class FakeAuthSessionStore : AuthSessionStore {

    var stored: StoredAuthSession? = null
        private set

    override suspend fun load(): StoredAuthSession? = stored

    override suspend fun save(session: StoredAuthSession) {
        stored = session
    }

    override suspend fun clear() {
        stored = null
    }
}
