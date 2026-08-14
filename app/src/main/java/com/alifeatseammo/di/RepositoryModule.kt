package com.alifeatseammo.di

import com.alifeatseammo.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = FirebaseAuthRepository()

    @Provides
    @Singleton
    fun provideGameRepository(): GameRepository = FirestoreGameRepository()

    @Provides
    @Singleton
    fun provideChatRepository(): ChatRepository = FirestoreChatRepository()

    @Provides
    @Singleton
    fun provideCrewRepository(): CrewRepository = FirestoreCrewRepository()

    @Provides
    @Singleton
    fun provideSocialRepository(): SocialRepository = FirestoreSocialRepository()

    @Provides
    @Singleton
    fun provideAdminRepository(): AdminRepository = FirestoreAdminRepository()

    @Provides
    @Singleton
    fun provideAuctionRepository(): AuctionRepository = FirestoreAuctionRepository()
}
