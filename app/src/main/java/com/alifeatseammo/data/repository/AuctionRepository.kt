package com.alifeatseammo.data.repository

import com.alifeatseammo.data.model.AuctionListing
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface AuctionRepository {
    fun getAuctionListings(): Flow<List<AuctionListing>>
    suspend fun listAuctionItem(itemId: String, price: Int): Boolean
    suspend fun buyAuctionItem(listingId: String): Boolean
    suspend fun cancelAuctionListing(listingId: String): Boolean
}

class FirestoreAuctionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) : AuctionRepository {

    override fun getAuctionListings(): Flow<List<AuctionListing>> = callbackFlow {
        val subscription = db.collection("auctions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("AuctionRepository", "Error fetching auctions", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<AuctionListing>()?.copy(id = doc.id) })
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun listAuctionItem(itemId: String, price: Int): Boolean {
        val data = hashMapOf(
            "itemId" to itemId,
            "price" to price
        )
        functions.getHttpsCallable("listAuctionItem").call(data).await()
        return true
    }

    override suspend fun buyAuctionItem(listingId: String): Boolean {
        val data = hashMapOf("listingId" to listingId)
        functions.getHttpsCallable("buyAuctionItem").call(data).await()
        return true
    }

    override suspend fun cancelAuctionListing(listingId: String): Boolean {
        val data = hashMapOf("listingId" to listingId)
        functions.getHttpsCallable("cancelAuctionListing").call(data).await()
        return true
    }
}
