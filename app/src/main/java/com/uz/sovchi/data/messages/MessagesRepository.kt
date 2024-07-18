package com.uz.sovchi.data.messages

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MessagesRepository {

    private val collectionName = "messages"
    private var messagesCollection = FirebaseFirestore.getInstance().collection(collectionName)

    fun loadMessages(
        startId: Long?, userId: String, limit: Int, result: (list: List<Message>) -> Unit
    ) {
        var query = messagesCollection.orderBy("date", Query.Direction.DESCENDING)
        if (startId != null) {
            query = query.startAfter(startId)
        }
        query.limit(limit.toLong()).whereEqualTo("userId", userId).get().addOnCompleteListener {
            try {
                val messages = it.result.mapNotNull {
                    try {
                        it.toObject(Message::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }.parseData()
                result.invoke(messages)
            } catch (e: Exception) {
                //
            }
        }
    }

    private fun List<Message>.parseData(): List<Message> {
        return map {
            it.apply {
                if (data is HashMap<*, *>) {
                    val hashMap = data as HashMap<*, *>
                    when (type) {
                        MESSAGE_TYPE_PLATFORM -> {
                            data = PlatformMessage(
                                hashMap[PlatformMessage::title.name].toString(),
                                hashMap[PlatformMessage::message.name].toString()
                            )
                        }

                        MESSAGE_TYPE_NOMZOD_FOR_YOU -> {
                            data = NomzodForYouModel(
                                hashMap["nomzodId"].toString(),
                                hashMap[NomzodForYouModel::nomzodName.name].toString(),
                                hashMap[NomzodForYouModel::nomzodAge.name].toString().toIntOrNull(),
                                hashMap[NomzodForYouModel::showPhoto.name].toString().toBoolean(),
                                hashMap[NomzodForYouModel::photo.name].toString()
                            )
                        }


                        MESSAGE_TYPE_NOMZOD_LIKED -> {
                            data = NomzodLikedModel(
                                hashMap["nomzodId"].toString(),
                                hashMap[NomzodLikedModel::likedUserName.name].toString(),
                                hashMap[NomzodLikedModel::likedUserId.name].toString(),
                                try {
                                    hashMap[NomzodLikedModel::hasNomzod.name] as Boolean
                                } catch (e: Exception) {
                                    false
                                },
                                try {
                                    hashMap[NomzodLikedModel::photo.name].toString()
                                } catch (e: Exception) {
                                    ""
                                }
                            )
                        }

                        MESSAGE_TYPE_NOMZOD_REQUEST -> {
                            data = NomzodRequestModel(
                                hashMap["nomzodId"].toString(),
                                hashMap[NomzodRequestModel::likedUserName.name].toString(),
                                hashMap[NomzodRequestModel::likedUserId.name].toString(),
                                try {
                                    hashMap[NomzodRequestModel::hasNomzod.name] as Boolean
                                } catch (e: Exception) {
                                    false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
