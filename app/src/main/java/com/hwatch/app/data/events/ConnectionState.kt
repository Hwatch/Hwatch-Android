package com.hwatch.app.data.events

/** SSE 连接状态。 */
enum class ConnectionState {
    CONNECTED,
    CONNECTING,
    RECONNECTING,
    DISCONNECTED,
}
