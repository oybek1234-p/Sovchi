package com.uz.sovchi.data.like

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.chat.ChatController
import com.uz.sovchi.data.chat.ChatMessageModel
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.utils.FirebaseUtils.toObjectSafe
import com.uz.sovchi.data.utils.FirebaseUtils.toObjectsSafe
import com.uz.sovchi.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.UUID

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

    fun getLikedMeCount(done: (count: Int) -> Unit) {
        var query = likesFullReference.orderBy(LikeModelFull::date.name, Query.Direction.DESCENDING)

        query = query.whereEqualTo(LikeModelFull::nomzodUserId.name, LocalUser.user.uid)
        query = query.whereEqualTo(LikeModelFull::likeState.name, LikeState.LIKED)
        query = query.whereEqualTo(LikeModelFull::matched.name, null)

        query.count().get(AggregateSource.SERVER).addOnSuccessListener {
            val count = it.count.toInt()
            done.invoke(count)
        }.addOnFailureListener {
            done.invoke(0)
        }
    }

    fun loadLikesFull(
        scope: CoroutineScope,
        lastNomzod: LikeModelFull?,
        endNomzod: LikeModelFull?,
        state: Int,
        done: (list: List<LikeModelFull>) -> Unit
    ) {
        var query = likesFullReference.limit(8)
            .orderBy(LikeModelFull::date.name, Query.Direction.DESCENDING)
        if (lastNomzod != null) {
            query = query.startAfter(lastNomzod.date)
        }
        if (endNomzod != null) {
            query = query.endBefore(endNomzod.date)
        }
        if (state == LikeState.MATCH) {
            query = query.whereEqualTo(LikeModelFull::userId.name, LocalUser.user.uid)
                .whereEqualTo(LikeModelFull::matched.name, true)
        } else {
            if (state == LikeState.LIKED_ME) {
                query = query.whereEqualTo(LikeModelFull::nomzodUserId.name, LocalUser.user.uid)
                query = query.whereEqualTo(LikeModelFull::likeState.name, LikeState.LIKED)
                query = query.whereEqualTo(LikeModelFull::matched.name, null)
            } else {
                query = query.whereEqualTo(LikeModelFull::userId.name, LocalUser.user.uid)
                    .whereEqualTo(LikeModelFull::likeState.name, state)
                query = query.whereEqualTo(LikeModelFull::matched.name, false)
            }
        }
        query.get().addOnSuccessListener {
            scope.launch {
                val list = it.toObjectsSafe(LikeModelFull::class.java)
                list.forEach { like ->
                    like.nomzod?.let { NomzodRepository.cacheNomzods[it.id] = it }
                }
                done.invoke(list)
            }
        }.addOnFailureListener {
            done.invoke(emptyList())
        }
    }

    data class LikeInfo(
        var iLiked: Boolean? = null,
        val likedMe: Boolean?,
        val dislikedMe: Boolean?,
        var matched: Boolean?
    )

    fun getLikeInfo(
        nomzodId: String, done: (info: LikeInfo) -> Unit
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
                done.invoke(LikeInfo(iLiked, likedMe, dislikedMe, matched))
            }
        }
        val currentUserId = LocalUser.user.uid
        val myId = currentUserId + nomzodId
        val theirId = nomzodId + currentUserId
        likesFullReference.document(myId).get().addOnSuccessListener {
            val doc = it.toObjectSafe(LikeModelFull::class.java)
            iLiked = doc?.likeState?.let { it == LikeState.LIKED }
            iLikedDone = true
            onDone.invoke()
        }.addOnFailureListener {
            iLikedDone = true
            onDone.invoke()
        }
        likesFullReference.document(theirId).get().addOnSuccessListener {
            val doc = it.toObjectSafe(LikeModelFull::class.java)
            likedMe = doc?.likeState?.let { it == LikeState.LIKED }
            dislikedMe = doc?.likeState?.let { it == LikeState.DISLIKED }
            likedMeDone = true
            onDone.invoke()
        }.addOnFailureListener {
            likedMeDone = true
            onDone.invoke()
        }
    }

    fun likeOrDislikeNomzod(userId: String, nomzod: Nomzod, likeState: Int) {
        if (MyNomzodController.nomzod.id.isEmpty()) return
        val myId = userId + nomzod.id
        val theirId = nomzod.userId + MyNomzodController.nomzod.id
        var theyLiked: Boolean? = null
        val addLike = {
            val matched = theyLiked == true && likeState == LikeState.LIKED
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
                if (theyLiked == null) null else matched,
                System.currentTimeMillis().toString()
            )
            if (likeState == LikeState.LIKED) {
                UserRepository.increaseLiked(nomzod.id, true)
            }
            if (theyLiked == true) {
                UserRepository.increaseLiked(LocalUser.user.uid, false)
                if (likeState == LikeState.LIKED) {
                    MainScope().launch {
                        ChatController.sendMessage(
                            ChatMessageModel(
                                System.currentTimeMillis().toString(),
                                UUID.randomUUID().toString(),
                                "Suxbatni boshlang",
                                LocalUser.user.uid,
                                nomzod.id,
                                FieldValue.serverTimestamp(),
                                "",
                                "",
                                deleted = true,
                                listOf(LocalUser.user.uid, nomzod.id),
                                false
                            )
                        ) { success, updated ->  }
                    }
                }
            }
            likesFullReference.document(myId).set(likeFull)
            if (theyLiked == true) {
                likesFullReference.document(theirId).update(
                    LikeModelFull::matched.name, (theyLiked == true) && likeState == LikeState.LIKED
                )
            }
        }
        likesFullReference.document(theirId).get().addOnSuccessListener {
            val doc = it.toObject(LikeModelFull::class.java)
            theyLiked = if (it.exists()) {
                doc?.likeState == LikeState.LIKED
            } else {
                null
            }
            addLike.invoke()
        }.addOnFailureListener {
            addLike.invoke()
        }
    }

}