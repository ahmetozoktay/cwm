package com.greatideasoft.cwm.domain.cards

import com.greatideasoft.cwm.domain.user.data.UserItem

data class MatchResult(
    val isMatch: Boolean,
    val conversationId: String? = null
)
