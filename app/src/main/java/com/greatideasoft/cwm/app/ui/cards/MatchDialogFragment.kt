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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.greatideasoft.cwm.app.R
import com.greatideasoft.cwm.app.databinding.DialogCardsMatchBinding

/**
 * This is the documentation block about the class
 */

class MatchDialogFragment: DialogFragment() {
	
	private var _binding: DialogCardsMatchBinding? = null
	private val binding: DialogCardsMatchBinding
		get() = _binding ?: throw IllegalStateException(
			"Trying to access the binding outside of the view lifecycle."
		)
	
	private var receivedName = ""
	private var receivedPhotoUrl = ""
	private var receivedConversationId = ""
	private var receivedPartnerId = ""
	
	companion object {

		private const val PHOTO_KEY = "PHOTO_URL"
		private const val NAME_KEY = "NAME"
		private const val CONVERSATION_ID_KEY = "CONVERSATION_ID"
		private const val PARTNER_ID_KEY = "PARTNER_ID"
		fun newInstance(name: String, photoUrl: String, conversationId: String, partnerId: String) = MatchDialogFragment().apply {
			arguments = Bundle().apply {
				putString(NAME_KEY, name)
				putString(PHOTO_KEY, photoUrl)
				putString(CONVERSATION_ID_KEY, conversationId)
				putString(PARTNER_ID_KEY, partnerId)
			}
		}
	}


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
		arguments?.let {
			receivedName = it.getString(NAME_KEY, "")
			receivedPhotoUrl = it.getString(PHOTO_KEY, "")
			receivedConversationId = it.getString(CONVERSATION_ID_KEY, "")
			receivedPartnerId = it.getString(PARTNER_ID_KEY, "")
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	) = DialogCardsMatchBinding.inflate(inflater, container, false).apply {
		this.name = receivedName
		this.photoUrl = receivedPhotoUrl
		_binding = this
		executePendingBindings()
	}.root


	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.tvMatchDialogBack.setOnClickListener { dialog?.dismiss() }
		binding.btnGoToChat.setOnClickListener {
			val bundle = Bundle().apply {
				putString("CONVERSATION_ID", receivedConversationId)
				putString("PARTNER_ID", receivedPartnerId)
				putString("PARTNER_NAME", receivedName)
				putString("PARTNER_PHOTO", receivedPhotoUrl)
			}
			findNavController().navigate(R.id.action_global_chatFragment, bundle)
			dialog?.dismiss()
		}
	}
}


