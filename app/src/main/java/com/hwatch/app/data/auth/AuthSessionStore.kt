package com.hwatch.app.data.auth

/** 会话持久化抽象（测试用内存实现，生产用 Keystore 加密实现）。 */
interface AuthSessionStore {
    suspend fun load(): StoredAuthSession?
    suspend fun save(session: StoredAuthSession)
    suspend fun clear()
}
