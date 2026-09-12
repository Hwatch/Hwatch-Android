package com.hwatch.app.ui.common

import androidx.annotation.StringRes
import com.hwatch.app.R
import com.hwatch.app.data.repository.ConnectionFailure
import com.hwatch.app.ui.dashboard.DashboardError

/** 把 Server Setup 的失败原因映射为用户可理解文案。 */
@StringRes
fun connectionFailureRes(failure: ConnectionFailure): Int = when (failure) {
    ConnectionFailure.INVALID_URL -> R.string.error_invalid_url
    ConnectionFailure.UNREACHABLE -> R.string.error_unreachable
    ConnectionFailure.TIMEOUT -> R.string.error_timeout
    ConnectionFailure.HTTP -> R.string.error_http
    ConnectionFailure.PARSE -> R.string.server_setup_error_parse
}

/** 把 Dashboard 的失败原因映射为用户可理解文案。 */
@StringRes
fun dashboardErrorRes(error: DashboardError): Int = when (error) {
    DashboardError.UNREACHABLE -> R.string.error_unreachable
    DashboardError.TIMEOUT -> R.string.error_timeout
    DashboardError.HTTP -> R.string.error_http
    DashboardError.PARSE -> R.string.error_parse
}
