package com.uz.sovchi.data.messages

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MessagesRepository {

    private val collectionName = "messages"
    private var messagesCollection = FirebaseFirestore.getInstance().collection(collectionName)

    fun loadMessages(
        startId: String?, userId: String, limit: Int, result: (list: List<Message>) -> Unit
    ) {
        messagesCollection.orderBy("id").orderBy("date",Query.Direction.DESCENDING).limit(limit.toLong())
            .startAfter(startId).whereEqualTo("userId", userId).get().addOnCompleteListener {
                val messages = it.result.toObjects(Message::class.java).parseData()
                result.invoke(messages)
            }
    }

    private fun List<Message>.parseData(): List<Message> {
        return map {
            it.apply {
                if (data is HashMap<*, *>) {
                    val hashMap = data as HashMap<*, *>
                    when (type) {
                        MESSAGE_TYPE_NOMZOD_FOR_YOU -> {
                            data = NomzodForYouModel(
                                hashMap["nomzodId"].toString(),
                                hashMap["title"].toString(),
                                hashMap["body"].toString()
                            )
                        }
                    }
                }
            }
        }
    }
}