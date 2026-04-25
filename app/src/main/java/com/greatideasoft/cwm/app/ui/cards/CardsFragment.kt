/*
 * Created by Andrii Kovalchuk
 * Copyright (C) 2022. cwm
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

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.greatideasoft.cwm.domain.user.data.UserItem
import com.greatideasoft.cwm.app.R
import com.greatideasoft.cwm.app.databinding.FragmentCardsBinding
import com.greatideasoft.cwm.app.ui.cards.CardsViewModel.SwipeAction.*
import com.greatideasoft.cwm.app.ui.common.ImagePagerAdapter
import com.greatideasoft.cwm.app.ui.common.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardsFragment: BaseFragment<CardsViewModel, FragmentCardsBinding>(
	layoutId = R.layout.fragment_cards
) {
	
	override val mViewModel: CardsViewModel by viewModels()
	
	private val mTopCardImagePagerAdapter = ImagePagerAdapter()
	private val mBottomCardImagePagerAdapter = ImagePagerAdapter()
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) = binding.run {
		observeTopCard()
		observeBottomCard()
		observeMatch()
		observeLoading()
		observeEmpty()

		motionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
			override fun onTransitionTrigger(layout: MotionLayout, triggerId: Int, positive: Boolean, progress: Float) {}
			override fun onTransitionStarted(layout: MotionLayout, start: Int, end: Int) {}
			override fun onTransitionChange(layout: MotionLayout, start: Int, end: Int, position: Float) {}
			
			override fun onTransitionCompleted(layout: MotionLayout, currentId: Int) {
				when (currentId) {
					R.id.topOffScreenSkip -> mViewModel.swipeTop(SKIP)
					R.id.topOffScreenLike -> mViewModel.swipeTop(LIKE)
					
					R.id.bottomOffScreenSkip -> mViewModel.swipeBottom(SKIP)
					R.id.bottomOffScreenLike -> mViewModel.swipeBottom(LIKE)
				}
			}
		})
	}
	
	/** top card setup ui*/
	private fun observeTopCard() = mViewModel.topCard.observe(viewLifecycleOwner, { setTopCard(it) })
	private fun setTopCard(userItem: UserItem?) = binding.topCard.run {
		if (userItem != null) root.run {
			
			mTopCardImagePagerAdapter.setData(userItem.photoURLs.map { it.fileUrl })
			
			vpCardPhotos.apply {
				adapter = mTopCardImagePagerAdapter
				isUserInputEnabled = false
			}
			
			TabLayoutMediator(tlCardPhotosIndicator, vpCardPhotos) { _: TabLayout.Tab, _: Int ->
				//do nothing
			}.attach()
			
			nextImage.setOnClickListener {
				val currentItem = vpCardPhotos.currentItem
				val totalItems = vpCardPhotos.adapter?.itemCount ?: 0
				if (currentItem < totalItems - 1) vpCardPhotos.currentItem = currentItem + 1
			}
			previousImage.setOnClickListener {
				val currentItem = vpCardPhotos.currentItem
				if (currentItem > 0) vpCardPhotos.currentItem = currentItem - 1
			}
			
			tvCardUserName.text = getString(R.string.name_age_formatter).format(
				userItem.baseUserInfo.name, userItem.baseUserInfo.age
			)
		}
	}
	
	
	/** bottom card setup ui*/
	private fun observeBottomCard() = mViewModel.bottomCard.observe(viewLifecycleOwner, { setBottomCard(it) })
	private fun setBottomCard(userItem: UserItem?) = binding.bottomCard.run {
		if (userItem != null) {
			
			mBottomCardImagePagerAdapter.setData(userItem.photoURLs.map { it.fileUrl })
			
			vpCardPhotos.apply {
				adapter = mBottomCardImagePagerAdapter
				isUserInputEnabled = false
			}
			
			TabLayoutMediator(tlCardPhotosIndicator, vpCardPhotos) { _: TabLayout.Tab, _: Int ->
				//do nothing
			}.attach()
			
			nextImage.setOnClickListener {
				val currentItem = vpCardPhotos.currentItem
				val totalItems = vpCardPhotos.adapter?.itemCount ?: 0
				if (currentItem < totalItems - 1) vpCardPhotos.currentItem = currentItem + 1
			}
			previousImage.setOnClickListener {
				val currentItem = vpCardPhotos.currentItem
				if (currentItem > 0) vpCardPhotos.currentItem = currentItem - 1
			}
			
			tvCardUserName.text = getString(R.string.name_age_formatter).format(
				userItem.baseUserInfo.name, userItem.baseUserInfo.age
			)
			
		}
	}
	
	private fun observeMatch() = mViewModel.showMatchDialog.observe(viewLifecycleOwner) { match ->
		match?.let {
			showMatchDialog(it.first, it.second)
			mViewModel.showMatchDialog.value = null
		}
	}

	private fun observeLoading() = mViewModel.showLoading.observe(viewLifecycleOwner) {
		binding.loadingView.visibility = if (it) View.VISIBLE else View.GONE
	}

	private fun observeEmpty() = mViewModel.showEmptyIndicator.observe(viewLifecycleOwner) {
		binding.tvCardHelperText.visibility = if (it) View.VISIBLE else View.GONE
	}

	private fun showMatchDialog(userItem: UserItem, conversationId: String) = MatchDialogFragment.newInstance(
		userItem.baseUserInfo.name, 
		userItem.baseUserInfo.mainPhotoUrl, 
		conversationId, 
		userItem.baseUserInfo.userId
	).show(childFragmentManager, MatchDialogFragment::class.java.canonicalName)
	
	
}



