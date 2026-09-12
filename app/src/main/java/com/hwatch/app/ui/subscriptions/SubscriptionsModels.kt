package com.hwatch.app.ui.subscriptions

import com.hwatch.app.data.model.CreatorDto
import com.hwatch.app.data.model.PlatformDto
import com.hwatch.app.data.model.SubscriptionDto

enum class SubscriptionsError {
    UNREACHABLE,
    TIMEOUT,
    HTTP,
    PARSE,
}

data class SubscriptionsMessage(
    val id: Long,
    val kind: Kind,
) {
    enum class Kind {
        CHECK_SUBMITTED,
        ACTION_FAILED,
        REFRESH_FAILED,
    }
}

data class SubscriptionUiState(
    val items: List<SubscriptionDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: SubscriptionsError? = null,
    val message: SubscriptionsMessage? = null,
    val pendingActions: Set<String> = emptySet(),
    val isCheckingAll: Boolean = false,
)

data class AddSubscriptionUiState(
    val platforms: List<PlatformDto> = emptyList(),
    val selectedPlatform: String? = null,
    val query: String = "",
    val results: List<CreatorDto> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: SubscriptionsError? = null,
    val url: String = "",
    val downloadExisting: Boolean = false,
    val isCreating: Boolean = false,
    val createError: SubscriptionsError? = null,
    val created: Boolean = false,
)
