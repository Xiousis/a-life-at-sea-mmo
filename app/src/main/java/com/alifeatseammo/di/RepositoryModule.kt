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
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository = FirebaseAuthRepository(auth)

    @Provides
    @Singleton
    fun provideGameRepository(db: FirebaseFirestore, functions: FirebaseFunctions): GameRepository = 
        FirestoreGameRepository(db, functions)

    @Provides
    @Singleton
    fun provideChatRepository(db: FirebaseFirestore, functions: FirebaseFunctions): ChatRepository = 
        FirestoreChatRepository(db, functions)

    @Provides
    @Singleton
    fun provideCrewRepository(db: FirebaseFirestore, functions: FirebaseFunctions): CrewRepository = 
        FirestoreCrewRepository(db, functions)

    @Provides
    @Singleton
    fun provideSocialRepository(db: FirebaseFirestore, functions: FirebaseFunctions): SocialRepository = 
        FirestoreSocialRepository(db, functions)

    @Provides
    @Singleton
    fun provideAdminRepository(db: FirebaseFirestore, functions: FirebaseFunctions): AdminRepository = 
        FirestoreAdminRepository(db, functions)

    @Provides
    @Singleton
    fun provideAuctionRepository(db: FirebaseFirestore, functions: FirebaseFunctions): AuctionRepository = 
        FirestoreAuctionRepository(db, functions)
}
