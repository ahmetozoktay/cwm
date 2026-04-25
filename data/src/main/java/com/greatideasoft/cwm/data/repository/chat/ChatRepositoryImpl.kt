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

package com.greatideasoft.cwm.data.repository.chat

import android.net.Uri
import android.text.format.DateFormat
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.getField
import com.google.firebase.storage.StorageReference
import com.greatideasoft.cwm.data.core.BaseRepository
import com.greatideasoft.cwm.data.core.MySchedulers
import com.greatideasoft.cwm.data.core.firebase.*
import com.greatideasoft.cwm.data.core.log.logDebug
import com.greatideasoft.cwm.data.core.log.logError
import com.greatideasoft.cwm.domain.PaginationDirection
import com.greatideasoft.cwm.domain.PaginationDirection.NEXT
import com.greatideasoft.cwm.domain.chat.ChatRepository
import com.greatideasoft.cwm.domain.chat.MessageItem
import com.greatideasoft.cwm.domain.conversations.ConversationItem
import com.greatideasoft.cwm.domain.photo.PhotoItem
import com.greatideasoft.cwm.domain.user.data.UserItem
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.internal.operators.observable.ObservableCreate
import java.util.*
import javax.inject.Inject

/**
 * [ChatRepository] implementation
 * [observeNewMessages] method uses firebase snapshot listener
 * read more about local changes and writing to backend with "latency compensation"
 * @link https://firebase.google.com/docs/firestore/query-data/listen
 */

class ChatRepositoryImpl @Inject constructor(
	private val fs: FirebaseFirestore,
	private val storage: StorageReference
): BaseRepository(), ChatRepository {
	
	companion object {
		private const val CONVERSATION_PARTNER_ONLINE_FIELD = "partnerOnline"
		private const val CONVERSATION_UNREAD_COUNT_FIELD = "unreadCount"
		private const val MESSAGES_COLLECTION = "messages"
		private const val MESSAGE_TIMESTAMP_FIELD = "timestamp"
		
		private const val LAST_MESSAGE_PHOTO = "Photo"
		
		private const val SOURCE_SERVER = "Server"
		private const val SOURCE_LOCAL = "Local"
	}
	
	private fun messagesQuery(conversation: ConversationItem): Query =
		fs.collection(CONVERSATIONS_COLLECTION)
			.document(conversation.conversationId)
			.collection(MESSAGES_COLLECTION)
			.orderBy(MESSAGE_TIMESTAMP_FIELD, Query.Direction.DESCENDING)
			.limit(20)
	
	private var isPartnerOnline = false
	
	/**
	 * this flag won't allow observable to emit new messages on initial loading
	 * for initial loading see [loadMessages] method
	 */
	private var isSnapshotInitiated = false
	
	override fun loadMessages(
		conversation: ConversationItem,
		lastMessage: MessageItem,
		direction: PaginationDirection
	): Single<List<MessageItem>> = (
		if (direction == NEXT) messagesQuery(conversation).startAfter(lastMessage.timestamp)
		else messagesQuery(conversation)
	).executeAndDeserializeSingle(MessageItem::class.java)
	
	
	override fun observeNewMessages(
        user: UserItem,
        conversation: ConversationItem
	): Observable<MessageItem> = ObservableCreate<MessageItem> { emitter ->
		val listener = fs.collection(CONVERSATIONS_COLLECTION)
			.document(conversation.conversationId)
			.collection(MESSAGES_COLLECTION)
			.orderBy(MESSAGE_TIMESTAMP_FIELD, Query.Direction.DESCENDING)
			.limit(1)
			.addSnapshotListener { snapshot, e ->
				if (e != null) {
					emitter.onError(e)
					return@addSnapshotListener
				}
				
				val source = if (snapshot != null && snapshot.metadata.hasPendingWrites()) SOURCE_LOCAL
				else SOURCE_SERVER
				
				if (!snapshot?.documents.isNullOrEmpty() && source == SOURCE_SERVER) {
					
					//documentChanges has various states, parse them
					snapshot!!.documentChanges.forEach { documentChange ->
						if (documentChange.type == DocumentChange.Type.ADDED) {
							
							val document = snapshot.documents.first()
							if (document.getField<String>("recipientId") == user.baseUserInfo.userId) {
								logDebug(TAG, "Message was sent not from this user")
								
								if (isSnapshotInitiated)
									emitter.onNext(document.toObject(MessageItem::class.java)!!)
							}
							
						}
					}
				}
				else {
					logDebug(TAG, "Message comes from $source source")
				}
				
				isSnapshotInitiated = true
				
			}
		emitter.setCancellable { listener.remove() }
	}.subscribeOn(MySchedulers.io())

	//override fun observePartnerOnline(conversationId: String): Observable<Boolean> =
	//	ObservableCreate<Boolean> { emitter ->
	//
	//		val listener = currentUserDocRef
	//			.collection(CONVERSATIONS_COLLECTION)
	//			.document(conversationId)
	//			.addSnapshotListener { snapshot, e ->
	//				if (e != null) {
	//					emitter.onError(e)
	//					return@addSnapshotListener
	//				}
	//				if (snapshot != null && snapshot.exists()) {
	//					snapshot.getBoolean(CONVERSATION_PARTNER_ONLINE_FIELD).let {
	//						if (isPartnerOnline != it) isPartnerOnline = it
	//						emitter.onNext(it)
	//					}
	//
	//				}
	//			}
	//		emitter.setCancellable { listener.remove() }
	//	}.subscribeOn(MySchedulers.io())

	override fun sendMessage(messageItem: MessageItem, emptyChat: Boolean): Completable {
		
		val conversation = fs
			.collection(CONVERSATIONS_COLLECTION)
			.document(messageItem.conversationId)

		messageItem.timestamp = FieldValue.serverTimestamp()

		return Completable.concatArray(
			//send message to server
			conversation.collection (MESSAGES_COLLECTION)
				.document()
				.setAsCompletable(messageItem),
			
			//update started status or skip
			if (emptyChat) updateStartedStatus(messageItem)
			else Completable.complete(),
			
			//update last message for both users
			updateLastMessage(messageItem)
		)
			//.andThen {
			//	updateUnreadMessagesCount(messageItem.conversationId)
			//}
	}
	
	private fun updateStartedStatus(message: MessageItem) = Completable.concatArray(
		// for current
		fs.collection(USERS_COLLECTION)
			.document(message.sender.userId)
			.collection(CONVERSATIONS_COLLECTION)
			.document(message.conversationId)
			.updateAsCompletable(CONVERSATION_STARTED_FIELD, true),
		
		fs.collection(USERS_COLLECTION)
			.document(message.sender.userId)
			.collection(USER_MATCHED_COLLECTION)
			.document(message.recipientId)
			.updateAsCompletable(CONVERSATION_STARTED_FIELD, true),
		
		// for partner
		fs.collection(USERS_COLLECTION)
			.document(message.recipientId)
			.collection(CONVERSATIONS_COLLECTION)
			.document(message.conversationId)
			.updateAsCompletable(CONVERSATION_STARTED_FIELD, true),
		
		fs.collection(USERS_COLLECTION)
			.document(message.recipientId)
			.collection(USER_MATCHED_COLLECTION)
			.document(message.sender.userId)
			.updateAsCompletable(CONVERSATION_STARTED_FIELD, true)
	
	).subscribeOn(MySchedulers.io())
	
	
	override fun uploadMessagePhoto(photoUri: String, conversationId: String): Single<PhotoItem> {
		val namePhoto = "${System.currentTimeMillis()}.jpg"
		val storageRef = storage
			.child(GENERAL_FOLDER_STORAGE_IMG)
			.child(CONVERSATIONS_FOLDER_STORAGE_IMG)
			.child(conversationId)
			.child(namePhoto)

		return Single.create { emitter ->
			logDebug(TAG, "Starting photo upload to bucket: ${storage.bucket}")
			logDebug(TAG, "Storage path: ${storageRef.path}")
			val uri = Uri.parse(photoUri)
			
			storageRef.putFile(uri)
				.continueWithTask { task ->
					if (!task.isSuccessful) {
						task.exception?.let { throw it }
					}
					storageRef.downloadUrl
				}
				.addOnSuccessListener { downloadUri ->
					logDebug(TAG, "Got download URL: $downloadUri")
					emitter.onSuccess(PhotoItem(fileName = namePhoto, fileUrl = downloadUri.toString()))
				}
				.addOnFailureListener {
					logError(TAG, "Upload or download URL failed: $it")
					emitter.onError(it)
				}
		}
	}
	
	
	/**
	 * update last messages in collections
	 * this last message directly relates to conversations
	 * when user enters a conversations fragment and see conversations list the last message is
	 * visible as last one received
	 * Updates can be called from both sides of chat participants, so here we can see a problem:
	 * when one user have bad internet connection and other sends message a collision expected
	 */
	private fun updateLastMessage(message: MessageItem): Completable {
		val cur = fs.collection(USERS_COLLECTION)
			.document(message.sender.userId)
			.collection(CONVERSATIONS_COLLECTION)
			.document(message.conversationId)

		val par = fs.collection(USERS_COLLECTION)
			.document(message.recipientId)
			.collection(CONVERSATIONS_COLLECTION)
			.document(message.conversationId)
		
		return if (message.photoItem != null) {
			Completable.concatArray(
				// for current
				cur.updateAsCompletable(CONVERSATION_LAST_MESSAGE_TEXT_FIELD, LAST_MESSAGE_PHOTO),
				cur.updateAsCompletable(CONVERSATION_TIMESTAMP_FIELD, message.timestamp),
				// for partner
				par.updateAsCompletable(CONVERSATION_LAST_MESSAGE_TEXT_FIELD, LAST_MESSAGE_PHOTO),
				par.updateAsCompletable(CONVERSATION_TIMESTAMP_FIELD, message.timestamp)
			)
		}
		else {
			Completable.concatArray(
				// for current
				cur.updateAsCompletable(CONVERSATION_LAST_MESSAGE_TEXT_FIELD, message.text),
				cur.updateAsCompletable(CONVERSATION_TIMESTAMP_FIELD, message.timestamp),
				// for partner
				par.updateAsCompletable(CONVERSATION_LAST_MESSAGE_TEXT_FIELD, message.text),
				par.updateAsCompletable(CONVERSATION_TIMESTAMP_FIELD, message.timestamp),
			)
		}
	}

	//update unread count if partner is not online in chat
	private fun updateUnreadMessagesCount(message: MessageItem) =
		fs.collection(USERS_COLLECTION)
			.document(message.recipientId)
			.collection(CONVERSATIONS_COLLECTION)
			.document(message.conversationId)
			.updateAsCompletable(CONVERSATION_UNREAD_COUNT_FIELD, FieldValue.increment(1))
}


