package com.uz.sovchi.data.like

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.showToast

object LikeController {
    private var likesFullReference = FirebaseFirestore.getInstance().collection("likesFull")

    fun removeLiked(nomzod: Nomzod) {
        val userId = LocalUser.user.uid
        try {
            likesFullReference.document(userId + nomzod.id).delete()
            likesFullReference.document(nomzod.userId + userId)
                .update(LikeModelFull::matched.name, false)
        } catch (e: Exception) {
            showToast(e.message.toString())
            return
        }
    }

    fun loadLikesFull(
        lastNomzod: LikeModelFull?, state: Int, done: (list: List<LikeModelFull>) -> Unit
    ) {
        var query =
            likesFullReference.limit(8).orderBy(LikeModelFull::id.name, Query.Direction.DESCENDING)
        if (lastNomzod != null) {
            query = query.startAfter(lastNomzod.id)
        }
        if (state == LikeState.LIKED_ME) {
            query = query.whereEqualTo(LikeModelFull::nomzodUserId.name, LocalUser.user.uid)
            query = query.whereEqualTo(LikeModelFull::likeState.name, LikeState.LIKED)
        } else {
            query = query.whereEqualTo(LikeModelFull::userId.name, LocalUser.user.uid)
                .whereEqualTo(LikeModelFull::likeState.name, state)
        }
        query.get().addOnCompleteListener {
            val list = it.result.toObjects(LikeModelFull::class.java)
            done.invoke(list)
        }
    }

    fun getLikeInfo(
        nomzod: Nomzod, done: (iLiked: Boolean?, likedMe: Boolean?, matched: Boolean?) -> Unit
    ) {
        var likedMeDone = false
        var iLikedDone = false
        var likedMe: Boolean? = false
        var iLiked: Boolean? = false
        var matched: Boolean
        val onDone = {
            if (likedMeDone && iLikedDone) {
                matched = likedMe == true && iLiked == true
                done.invoke(iLiked, likedMe, matched)
            }
        }
        val currentUserId = LocalUser.user.uid
        val myId = currentUserId + nomzod.id
        val theirId = nomzod.id + currentUserId
        likesFullReference.document(myId).get().addOnSuccessListener {
            val doc = it.toObject(LikeModelFull::class.java)
            iLiked = doc?.likeState?.let { it == LikeState.LIKED }
            iLikedDone = true
            onDone.invoke()
        }.addOnFailureListener {
            iLikedDone = true
            onDone.invoke()
        }
        likesFullReference.document(theirId).get().addOnSuccessListener {
            val doc = it.toObject(LikeModelFull::class.java)
            likedMe = doc?.likeState?.let { it == LikeState.LIKED }
            likedMeDone = true
            onDone.invoke()
        }
    }

    fun likeOrDislikeNomzod(userId: String, nomzod: Nomzod, likeState: Int) {
        val myId = userId + nomzod.id
        val theirId = nomzod.userId + MyNomzodController.nomzod.id
        var theyLiked = false
        val addLike = {
            val matched = theyLiked && likeState == LikeState.LIKED
            val likeFull = LikeModelFull(
                myId,
                userId,
                LocalUser.user.name,
                LocalUser.user.hasNomzod,
                MyNomzodController.nomzod.photos.firstOrNull() ?: "",
                nomzod.id,
                nomzod.userId,
                nomzod,
                likeState,
                matched
            )
            likesFullReference.document(myId).set(likeFull)
            if (matched) {
                likesFullReference.document(theirId).update(LikeModelFull::matched.name, true)
            }
        }
        likesFullReference.document(theirId).get().addOnSuccessListener {
            val doc = it.toObject(LikeModelFull::class.java)
            theyLiked = doc?.likeState == LikeState.LIKED
            addLike.invoke()
        }.addOnFailureListener {
            addLike.invoke()
        }

    }
}