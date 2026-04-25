/*
 * Created by Andrii Kovalchuk
 * Copyright (C) 2021. cwm
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses
 */

package com.greatideasoft.cwm.app.ui.cards

import androidx.lifecycle.MutableLiveData
import com.greatideasoft.cwm.domain.cards.CardsRepository
import com.greatideasoft.cwm.domain.cards.MatchResult
import com.greatideasoft.cwm.domain.user.data.UserItem
import com.greatideasoft.cwm.app.core.log.logDebug
import com.greatideasoft.cwm.app.core.log.logInfo
import com.greatideasoft.cwm.app.core.log.logError
import com.greatideasoft.cwm.app.ui.MainActivity
import com.greatideasoft.cwm.app.ui.cards.CardsViewModel.SwipeAction.*
import com.greatideasoft.cwm.app.ui.common.base.BaseViewModel
import com.greatideasoft.cwm.app.ui.common.errors.ErrorType
import com.greatideasoft.cwm.app.ui.common.errors.MyError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardsViewModel @Inject constructor(
	private val repo: CardsRepository
): BaseViewModel() {
	
	enum class SwipeAction {
		SKIP, LIKE
	}
	
	private var cardIndex = 0
	private val usersCardsList = MutableLiveData<List<UserItem>>()

	val showLoading = MutableLiveData<Boolean>()
	val showMatchDialog = MutableLiveData<Pair<UserItem, String>>()
	val showEmptyIndicator = MutableLiveData<Boolean>()
	
	
	val topCard = MutableLiveData<UserItem?>(null)
	val bottomCard = MutableLiveData<UserItem?>(null)
	
	init {
		loadUsersByPreferences(true)
	}

	private fun addToSkipped(skippedUser: UserItem) {
		disposables.add(repo.skipUser(MainActivity.currentUser!!, skippedUser)
            .observeOn(mainThread()).subscribe(
				{ logDebug(TAG, "Skipped: $skippedUser") },
				{ error.value = MyError(ErrorType.SUBMITING, it) }
			)
		)
	}


	private fun checkMatch(likedUser: UserItem) {
		disposables.add(repo.likeUserAndCheckMatch(MainActivity.currentUser!!, likedUser)
            .observeOn(mainThread())
            .subscribe(
	            { if (it.isMatch) showMatchDialog.value = Pair(likedUser, it.conversationId!!) },
	            { error.value = MyError(ErrorType.CHECKING, it) }
			)
		)
	}

	private fun loadUsersByPreferences(initialLoading: Boolean = false) {
		disposables.add(repo.getUsersByPreferences(MainActivity.currentUser!!, initialLoading)
            .observeOn(mainThread())
            .doOnSubscribe { if (initialLoading) showLoading.value = true }
			.doFinally { showLoading.value = false }
            .subscribe(
				{ cards ->
					showEmptyIndicator.postValue(false)
					showLoading.postValue(false)
					
					if (cards.isNotEmpty()) {
						cardIndex = 0
						
						usersCardsList.postValue(cards)
						
						topCard.postValue(cards.first())
						bottomCard.postValue(cards.drop(1).firstOrNull())
						showEmptyIndicator.postValue(false)
					}
					else {
						topCard.postValue(null)
						bottomCard.postValue(null)
						showEmptyIndicator.postValue(true)
					}
					
					logInfo(TAG, "loaded cards: ${cards.size}")
				},
				{ 
					logError(TAG, "Error loading cards: ${it.localizedMessage}")
					showEmptyIndicator.postValue(true)
					// Silent error for LOADING type to avoid annoying dialogs
					// error.value = MyError(ErrorType.LOADING, it)
				}
			)
		)
	}
	
	fun swipeTop(swipeAction: SwipeAction) {
		cardIndex += 2
		when (swipeAction) {
			SKIP -> topCard.value?.let { addToSkipped(it) }
			LIKE -> topCard.value?.let { checkMatch(it) }.also {
				logInfo(TAG, "Liked top: ${topCard.value?.baseUserInfo?.name}")
			}
		}
		
		if (cardIndex >= usersCardsList.value!!.size) loadUsersByPreferences()
		else topCard.postValue(usersCardsList.value!!.getOrNull(cardIndex))
		
	}
	
	fun swipeBottom(swipeAction: SwipeAction) {
		when (swipeAction) {
			SKIP -> bottomCard.value?.let { addToSkipped(it) }
			LIKE -> bottomCard.value?.let { checkMatch(it) }.also {
				logInfo(TAG, "Liked bottom: ${bottomCard.value?.baseUserInfo?.name}")
			}
		}
		bottomCard.postValue(usersCardsList.value!!.getOrNull(cardIndex + 1))
	}
	
}




