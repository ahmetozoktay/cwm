package com.greatideasoft.cwm.data.core.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.greatideasoft.cwm.data.datasource.UserDataSource
import com.greatideasoft.cwm.data.datasource.location.LocationDataSource
import com.greatideasoft.cwm.data.repository.auth.AuthRepositoryImpl
import com.greatideasoft.cwm.data.repository.cards.CardsRepositoryImpl
import com.greatideasoft.cwm.data.repository.chat.ChatRepositoryImpl
import com.greatideasoft.cwm.data.repository.conversations.ConversationsRepositoryImpl
import com.greatideasoft.cwm.data.repository.pairs.PairsRepositoryImpl
import com.greatideasoft.cwm.data.repository.user.SettingsRepositoryImpl
import com.greatideasoft.cwm.data.repository.user.UserRepositoryImpl
import com.greatideasoft.cwm.domain.auth.AuthRepository
import com.greatideasoft.cwm.domain.cards.CardsRepository
import com.greatideasoft.cwm.domain.chat.ChatRepository
import com.greatideasoft.cwm.domain.conversations.ConversationsRepository
import com.greatideasoft.cwm.domain.pairs.PairsRepository
import com.greatideasoft.cwm.domain.user.ISettingsRepository
import com.greatideasoft.cwm.domain.user.IUserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCardsRepository(impl: CardsRepositoryImpl): CardsRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindConversationsRepository(impl: ConversationsRepositoryImpl): ConversationsRepository

    @Binds
    @Singleton
    abstract fun bindPairsRepository(impl: PairsRepositoryImpl): PairsRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): IUserRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): ISettingsRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance("gs://comewithme-b2350.firebasestorage.app")

        @Provides
        @Singleton
        fun provideFirebaseStorageReference(storage: FirebaseStorage): StorageReference = storage.reference

        @Provides
        @Singleton
        fun provideUserDataSource(fs: FirebaseFirestore): UserDataSource = UserDataSource(fs)

        @Provides
        @Singleton
        fun provideLocationDataSource(@ApplicationContext context: Context): LocationDataSource = LocationDataSource(context)
    }
}
