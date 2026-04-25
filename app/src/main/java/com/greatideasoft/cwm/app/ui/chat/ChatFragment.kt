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

package com.greatideasoft.cwm.app.ui.chat

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greatideasoft.cwm.domain.PaginationDirection.*
import com.greatideasoft.cwm.domain.chat.MessageItem
import com.greatideasoft.cwm.domain.conversations.ConversationItem
import com.greatideasoft.cwm.domain.pairs.MatchedUserItem
import com.greatideasoft.cwm.domain.photo.PhotoItem
import com.greatideasoft.cwm.domain.user.data.BaseUserInfo
import com.greatideasoft.cwm.domain.user.data.ReportType.*
import com.greatideasoft.cwm.domain.user.data.UserItem
import com.greatideasoft.cwm.app.R
import com.greatideasoft.cwm.app.core.permissions.AppPermission
import com.greatideasoft.cwm.app.core.permissions.AppPermission.PermissionCode
import com.greatideasoft.cwm.app.core.permissions.handlePermission
import com.greatideasoft.cwm.app.core.permissions.onRequestPermissionsResultReceived
import com.greatideasoft.cwm.app.core.permissions.requestAppPermissions
import com.greatideasoft.cwm.app.databinding.FragmentChatBinding
import com.greatideasoft.cwm.app.ui.MainActivity
import com.greatideasoft.cwm.app.core.log.logDebug
import com.greatideasoft.cwm.app.ui.common.base.BaseFragment
import com.greatideasoft.cwm.app.ui.profile.RemoteRepoViewModel
import com.greatideasoft.cwm.app.utils.extensions.hideKeyboard
import com.greatideasoft.cwm.app.utils.extensions.observeOnce
import com.greatideasoft.cwm.app.utils.extensions.showToastText
import dagger.hilt.android.AndroidEntryPoint
import com.greatideasoft.cwm.app.BuildConfig
import java.io.File
import java.util.*


/**
 * This is the documentation block about the class
 */

@AndroidEntryPoint
class ChatFragment : BaseFragment<ChatViewModel, FragmentChatBinding>(
	layoutId = R.layout.fragment_chat
) {
	
	override val mViewModel: ChatViewModel by viewModels()
	private val remoteRepoViewModel: RemoteRepoViewModel by viewModels()

	private var receivedPartnerCity = ""
	private var receivedPartnerGender = ""
	private var receivedPartnerId = ""
	private var receivedConversationId = ""
	
	private var isReported: Boolean = false

	private lateinit var currentConversation: ConversationItem
	private lateinit var currentPartner: UserItem

	private val mChatAdapter: ChatAdapter = ChatAdapter().apply {
		//set current user id to understand left/right message
		setCurrentUserId(MainActivity.currentUser!!.baseUserInfo.userId)
		
		setLoadPrevListener { message ->
			mViewModel.loadMessages(currentConversation, message, NEXT)
		}
	}

	// File
	private lateinit var mFilePathImageCamera: File

	

	//static fields
	companion object {
		private const val PARTNER_CITY_KEY = "PARTNER_CITY"
		private const val PARTNER_GENDER_KEY = "PARTNER_GENDER"
		private const val PARTNER_ID_KEY = "PARTNER_ID"
		private const val CONVERSATION_ID_KEY = "CONVERSATION_ID"

	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		//deep link from notification
		arguments?.let { handleDeepLink(it) }
	}
	
	private fun observePrevMessages() = mViewModel.nextMessages.observe(viewLifecycleOwner, {
		mChatAdapter.insertPrev(it)
	})
	
	private fun observeNewMessage() = mViewModel.newMessage.observe(viewLifecycleOwner, {
		mChatAdapter.newMessage(it)
		binding.rvMessageList.scrollToPosition(0)
	})
	
	private fun handleDeepLink(bundle: Bundle) {
		receivedPartnerCity = bundle.getString(PARTNER_CITY_KEY, "")
		receivedPartnerGender = bundle.getString(PARTNER_GENDER_KEY, "")
		receivedPartnerId = bundle.getString(PARTNER_ID_KEY, "")
		receivedConversationId = bundle.getString(CONVERSATION_ID_KEY, "")
		
		// Also check for direct navigation from MatchDialog
		if (receivedPartnerId.isEmpty()) receivedPartnerId = bundle.getString("PARTNER_ID", "")
		if (receivedConversationId.isEmpty()) receivedConversationId = bundle.getString("CONVERSATION_ID", "")

		if (receivedPartnerId.isNotEmpty() && receivedConversationId.isNotEmpty()) {
			
			// Initialize currentConversation immediately if we have data to avoid UninitializedPropertyAccessException
			currentConversation = ConversationItem(
				partner = BaseUserInfo(
					userId = receivedPartnerId,
					name = bundle.getString("PARTNER_NAME", ""),
					mainPhotoUrl = bundle.getString("PARTNER_PHOTO", "")
				),
				conversationId = receivedConversationId,
				conversationStarted = true
			)
			
			// If we have partner data, we can also initialize currentPartner (at least with base info)
			// This is a bit risky since currentPartner is UserItem, but better than a crash.
			// Ideally, we'd fetch the full UserItem, which remoteRepoViewModel.getRequestedUserInfo(it.partner) does.

			//if it was a deep link or manual navigation then create ConversationItem "on a flight"
			
			sharedViewModel.matchedUserItemSelected.value =
				MatchedUserItem(
					baseUserInfo = BaseUserInfo(
						userId = receivedPartnerId,
						name = bundle.getString("PARTNER_NAME", ""),
						mainPhotoUrl = bundle.getString("PARTNER_PHOTO", "")
					),
					conversationId = receivedConversationId,
					conversationStarted = true
				)
			sharedViewModel.conversationSelected.value = currentConversation
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) = binding.run {
		
		// Setup observers in onViewCreated to avoid LifecycleOwner issues
		remoteRepoViewModel.retrievedUserItem.observe(viewLifecycleOwner, {
			currentPartner = it
			mViewModel.partnerName.value = it.baseUserInfo.name.split(" ")[0]
			mViewModel.partnerPhoto.value = it.baseUserInfo.mainPhotoUrl
		})
		
		remoteRepoViewModel.reportSubmittingStatus.observe(viewLifecycleOwner, {
			isReported = it
			requireContext().showToastText(getString(R.string.toast_text_report_success))
		})
		
		sharedViewModel.conversationSelected.observe(viewLifecycleOwner, {
			currentConversation = it
			remoteRepoViewModel.getRequestedUserInfo(it.partner)
			//start listening for new messages
			mViewModel.observeNewMessages(it)
			//mViewModel.observePartnerOnline(it.conversationId)
			//init loading chat messages
			mViewModel.loadMessages(it, MessageItem(), INITIAL)
		})
		//set init messages list in adapter
		mViewModel.initMessages.observe(viewLifecycleOwner, {
			mChatAdapter.setNewData(it)
		})
		observePrevMessages()
		observeNewMessage()

		edTextMessageInput.doOnTextChanged { text, start, before, count ->
			btnSendMessage.isActivated = text?.trim().isNullOrBlank()
		}

		btnSendMessage.setOnClickListener { sendMessageClick() }

		//show attachment dialog picker
		btnSendAttachment.setOnClickListener {
			MaterialAlertDialogBuilder(requireContext())
				.setItems(
					arrayOf(
						getString(R.string.material_dialog_picker_camera),
						getString(R.string.material_dialog_picker_gallery)
					)
				) { _, itemIndex ->
					when (itemIndex) {
						0 -> photoCameraClick()
						1 -> photoGalleryClick()
					}
				}
				.create()
				.apply { window?.attributes?.gravity = Gravity.BOTTOM }
				.show()
		}

		//if message contains photo then it opens in fullscreen dialog
		mChatAdapter.setOnAttachedPhotoClickListener { view, position, photoItem ->
			photoItem?.fileUrl?.let {
				FullScreenDialogFragment.newInstance(it)
					.show(childFragmentManager, FullScreenDialogFragment::class.java.canonicalName)
			}
		}
		
		
		rvMessageList.apply {
			adapter = mChatAdapter
			layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
			setOnTouchListener { v, _ ->
				v.performClick()
				v.hideKeyboard(edTextMessageInput)
			}
		}

		toolbarChat.setNavigationOnClickListener { onBackPressed() }

		toolbarChat.setOnMenuItemClickListener { item ->
			when (item.itemId) {
				R.id.chat_action_report -> { if (!isReported) showReportDialog() }
			}
			return@setOnMenuItemClickListener true
		}

		toolbarInnerContainer.setOnClickListener {
			navController.navigate(R.id.action_chat_to_profileFragment)
		}
	}

	/**
	 * Send plain text msg to chat if editText is not empty
	 * else shake animation
	 */
	private fun sendMessageClick() = binding.run {
		
		if (edTextMessageInput.text.toString().trim().isNotEmpty()) {
			if (!::currentConversation.isInitialized) {
				requireContext().showToastText("Conversation not initialized yet")
				return@run
			}
			val text = edTextMessageInput.text.toString().trim()
			
			val message = mViewModel.sendMessage(text, currentConversation)
			
			//update local appearance (to avoid back pressure)
			//there is no guarantee that message will be delivered
			mChatAdapter.newMessage(message)
			rvMessageList.scrollToPosition(0)
			edTextMessageInput.text?.clear()
		}
		else edTextMessageInput.startAnimation(AnimationUtils.loadAnimation(context, R.anim.horizontal_shake))

	}

	/**
	 * Checks if the app has permissions to OPEN CAMERA and take photos
	 * If the app does not has permission then the user will be prompted to grant permissions
	 * else open camera intent
	 */
	private fun photoCameraClick() = handlePermission(
		AppPermission.CAMERA,
		onGranted = { startCameraIntent() },
		onDenied = { requestAppPermissions(it) },
		onExplanationNeeded = { it.explanationMessageId }
	)

	//take photo directly by camera
	private fun startCameraIntent() {
		val namePhoto = DateFormat.format("yyyy-MM-dd_hhmmss", Date()).toString()
		mFilePathImageCamera = File(
			requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), namePhoto + "camera.jpg")
		val photoURI = FileProvider.getUriForFile(
			requireContext(),
			BuildConfig.APPLICATION_ID + ".provider",
			mFilePathImageCamera
		)
		val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
			putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
		}
		startActivityForResult(intent, PermissionCode.REQUEST_CODE_CAMERA.code)
	}

	/**
	 * Checks if the app has permissions to READ user files
	 * If the app does not has permission then the user will be prompted to grant permissions
	 * else open gallery to choose photo
	 */
	private fun photoGalleryClick() = handlePermission(
		AppPermission.GALLERY,
		onGranted = { startGalleryIntent() },
		onDenied = { requestAppPermissions(it) },
		onExplanationNeeded = { it.explanationMessageId }
	)
	

	//open gallery chooser
	private fun startGalleryIntent() {
		val intent = Intent().apply {
			action = Intent.ACTION_GET_CONTENT
			type = "image/*"
		}
		startActivityForResult(Intent.createChooser(intent, "Select image"), PermissionCode.REQUEST_CODE_GALLERY.code)
	}

	// start after permissions was granted
	// If request is cancelled, the result arrays are empty.
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		onRequestPermissionsResultReceived(
			requestCode,
			grantResults,
			onPermissionGranted = {
				when (it) {
					AppPermission.CAMERA -> startCameraIntent()
					AppPermission.GALLERY -> startGalleryIntent()
					else -> {}
				}
			},
			onPermissionDenied = {
				requireContext().showToastText(getString(it.deniedMessageId))
			}
		)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		//check if response with status 'OK'
		if (resultCode == RESULT_OK) {
			
			//check which request that was
			val localUri = when (requestCode) {
				PermissionCode.REQUEST_CODE_GALLERY.code -> {
					
					val galleryUri = data?.data.toString()
					logDebug(TAG, "Gallery URI: $galleryUri for conversation: ${currentConversation.conversationId}")
					// send photo from gallery
					mViewModel.sendPhoto(galleryUri, currentConversation)
					
					galleryUri
				}
				
				PermissionCode.REQUEST_CODE_CAMERA.code -> {
					if (mFilePathImageCamera.exists()) {
						val cameraUri = Uri.fromFile(mFilePathImageCamera).toString()
						logDebug(TAG, "Camera URI: $cameraUri for conversation: ${currentConversation.conversationId}")
						// send photo taken by camera
						mViewModel.sendPhoto(cameraUri, currentConversation)
						
						cameraUri
					}
					else { requireContext().showToastText("filePathImageCamera is null or filePathImageCamera isn't exists")
						""
					}
					
				}
				else -> { "" }
			}
			
			val photoMessage = MessageItem(
				sender = MainActivity.currentUser!!.baseUserInfo,
				recipientId = currentConversation.partner.userId,
				photoItem = PhotoItem(fileUrl = localUri),
				conversationId = currentConversation.conversationId
			)
			mChatAdapter.newMessage(photoMessage).also {
				binding.rvMessageList.scrollToPosition(0)
			}
		}
		else {
			requireContext().showToastText(resultCode.toString())
		}
		
	}

	private fun showReportDialog() = MaterialAlertDialogBuilder(requireContext())
		.setItems(
			arrayOf(
				getString(R.string.report_chooser_photos),
				getString(R.string.report_chooser_behavior),
				getString(R.string.report_chooser_fake)
			)
		) { _, itemIndex ->
			when (itemIndex) {
				0 -> remoteRepoViewModel.submitReport(INELIGIBLE_PHOTOS, currentPartner.baseUserInfo)
				1 -> remoteRepoViewModel.submitReport(DISRESPECTFUL_BEHAVIOR, currentPartner.baseUserInfo)
				2 -> remoteRepoViewModel.submitReport(FAKE, currentPartner.baseUserInfo)
				
			}
		}
		.create()
		.apply { window?.attributes?.gravity = Gravity.CENTER }
		.show()
	
	/**
	 * if we enter chat from pairs fragment and start the conversation we should go back
	 * not to pairs fragment but to conversations
	 * if conversation is not started so return to profile and then to pairs
	 */
	override fun onBackPressed() {
		if (mViewModel.chatIsEmpty.value == false) {
			navController.popBackStack(R.id.conversationsFragment, false)
		} else {
			navController.navigateUp()
		}
	}

}



