package com.uz.sovchi.data.like

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.UserRepository
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
        var query = likesFullReference.limit(8)
            .orderBy(LikeModelFull::date.name, Query.Direction.DESCENDING)
        if (lastNomzod != null) {
            query = query.startAfter(lastNomzod.date)
        }
        if (state == LikeState.MATCH) {
            query = query.whereEqualTo(LikeModelFull::userId.name, LocalUser.user.uid)
                .whereEqualTo(LikeModelFull::matched.name, true)
        } else {
            if (state == LikeState.LIKED_ME) {
                query = query.whereEqualTo(LikeModelFull::nomzodUserId.name, LocalUser.user.uid)
                query = query.whereEqualTo(LikeModelFull::likeState.name, LikeState.LIKED)
                query = query.whereEqualTo(LikeModelFull::matched.name, false)
            } else {
                query = query.whereEqualTo(LikeModelFull::userId.name, LocalUser.user.uid)
                    .whereEqualTo(LikeModelFull::likeState.name, state)
                query = query.whereEqualTo(LikeModelFull::matched.name, false)
            }
        }
        query.get().addOnSuccessListener {
            val list = it.toObjects(LikeModelFull::class.java)
            done.invoke(list)
        }.addOnFailureListener {
            done.invoke(emptyList())
        }
    }

    fun getLikeInfo(
        nomzod: Nomzod,
        done: (iLiked: Boolean?, likedMe: Boolean?, dislikedMe: Boolean?, matched: Boolean?) -> Unit
    ) {
        var likedMeDone = false
        var iLikedDone = false
        var likedMe: Boolean? = false
        var iLiked: Boolean? = false
        var dislikedMe: Boolean? = false
        var matched: Boolean
        val onDone = {
            if (likedMeDone && iLikedDone) {
                matched = likedMe == true && iLiked == true
                done.invoke(iLiked, likedMe, dislikedMe, matched)
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
            dislikedMe = doc?.likeState?.let { it == LikeState.DISLIKED }
            likedMeDone = true
            onDone.invoke()
        }
    }

    fun likeOrDislikeNomzod(userId: String, nomzod: Nomzod, likeState: Int) {
        if (MyNomzodController.nomzod.id.isEmpty()) return
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
                MyNomzodController.nomzod,
                likeState,
                matched,
                System.currentTimeMillis().toString()
            )
            likesFullReference.document(myId).set(likeFull)
            if (likeState == LikeState.LIKED) {
                UserRepository.increaseRequest()
            }
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