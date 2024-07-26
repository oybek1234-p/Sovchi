package com.uz.sovchi.data

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.messaging
import com.uz.sovchi.appContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object UserRepository {

    init {
        LocalUser.getUser(appContext)
    }

    val usersReference: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("users")
    }

    val user: User get() = LocalUser.user
    private var userLoading = false

    private fun saveUserNetwork() {
        if (user.valid) {
            usersReference.child(user.uid).setValue(user)
        }
    }

    fun removeLastSeenListener(listener: ValueEventListener) {
        usersReference.child(user.uid).child(User::lastSeenTime.name).removeEventListener(listener)
    }

    fun setPremium(userId: String, premium: Boolean) {
        if (userId.isEmpty()) return
        usersReference.child(userId).updateChildren(
            mapOf(
                User::premium.name to premium, User::premiumDate.name to System.currentTimeMillis()
            )
        )
    }

    fun observeLastSeen(userId: String, onChange: (time: Long) -> Unit): ValueEventListener? {
        if (userId.isEmpty()) return null
        val listener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Long::class.java)
                if (value is Long) {
                    onChange.invoke(value)
                }
            }
        }
        usersReference.child(userId).child(User::lastSeenTime.name).addValueEventListener(listener)
        return listener
    }

    fun getLastSeen(userId: String, done: (time: Long) -> Unit) {
        try {
            usersReference.child(userId).child(User::lastSeenTime.name).get()
                .addOnCompleteListener {
                    val time = it.result.getValue(Long::class.java) ?: System.currentTimeMillis()
                    done.invoke(time)
                }
        } catch (e: Exception) {
            //
        }
    }

    fun loadAllUsers(done: (list: List<User>) -> Unit) {
        usersReference.get().addOnCompleteListener { it ->
            done.invoke(it.result.children.mapNotNull { it.getValue(User::class.java) })
        }
    }

    fun setUnreadNotificationsZero() {
        if (user.valid) {
            if (user.unreadMessages == 0) return
            usersReference.child(user.uid).child(User::unreadMessages.name).setValue(0)
        }
    }

    fun setUnreadChatZero() {
        if (user.valid) {
            if (user.unreadChats == 0) return
            usersReference.child(user.uid).child(User::unreadChats.name).setValue(0)
        }
    }

    fun observeUnChatMessages(onChange: (count: Int) -> Unit) {
        if (user.valid.not()) return
        usersReference.child(user.uid).child(User::unreadChats.name)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {

                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue(Int::class.java)
                    if (value is Int) {
                        LocalUser.user.unreadChats = value
                        LocalUser.saveUser()
                        onChange.invoke(value)
                    }
                }
            })
    }

    fun observeUnReadMessages(onChange: (count: Int) -> Unit) {
        if (user.valid.not()) return
        usersReference.child(user.uid).child(User::unreadMessages.name)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {

                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue(Int::class.java)
                    if (value is Int) {
                        LocalUser.user.unreadMessages = value
                        LocalUser.saveUser()
                        onChange.invoke(value)
                    }
                }
            })
    }

    fun signOut() {
        Firebase.messaging.unsubscribeFromTopic(LocalUser.user.uid + "topic")
        FirebaseAuth.getInstance().signOut()
        LocalUser.user = User()
        LocalUser.saveUser(appContext)
    }

    private fun createNewUserAndSet(user: User) {
        if (user.valid) {
            LocalUser.user = user
            LocalUser.saveUser(appContext)

            saveUserNetwork()
        }
    }

    fun updateUser(user: User) {
        if (user.valid && user.uid == LocalUser.user.uid) {
            LocalUser.user = user
            LocalUser.saveUser(appContext)

            saveUserNetwork()
        }
    }

    fun setHasNomzod(has: Boolean) {
        if (user.valid.not()) return
        user.hasNomzod = has
        updateUser(user)
    }

    fun updateLastSeenTime() {
        if (user.valid) {
            val lastSeen = System.currentTimeMillis()
            LocalUser.user.lastSeenTime = lastSeen
            LocalUser.saveUser()
            usersReference.child(user.uid).child(User::lastSeenTime.name).setValue(lastSeen)
        }
    }

    suspend fun authFirebaseUser(): User? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null || firebaseUser.uid.isEmpty()) {
            return null
        }
        val id = firebaseUser.uid
        val isNewUser: Boolean
        val networkUser = loadUser(id)
        isNewUser = networkUser.valid.not()
        return if (isNewUser) {
            val newUser = User(
                uid = id,
                "",
                firebaseUser.phoneNumber ?: "",
                System.currentTimeMillis(),
                false,
                0,
                false,
                0,
                0,
                false,
                0
            )
            createNewUserAndSet(newUser)
            newUser
        } else {
            LocalUser.user = networkUser!!
            LocalUser.saveUser(appContext)
            networkUser
        }
    }

    private suspend fun loadUser(id: String) = suspendCoroutine<User?> { sus ->
        val reference = usersReference.child(id)
        reference.get().addOnCompleteListener {
            val loadedUser = it.result.getValue(User::class.java)
            sus.resume(loadedUser)
        }
    }

    fun increaseRequest() {
        if (user.valid) {
            if (user.requests < 5) {
                user.requests += 1
                updateUser(user)
            }
        }
    }

    suspend fun loadCurrentUser() = suspendCoroutine<Result<User>> { sus ->
        if (userLoading || user.valid.not()) {
            sus.resume(Result.failure(Throwable()))
            return@suspendCoroutine
        }
        userLoading = true
        val reference = usersReference.child(user.uid)
        reference.get().addOnCompleteListener {
            userLoading = false
            val loadedUser = it.result.getValue(User::class.java)
            if (loadedUser.valid) {
                LocalUser.user = loadedUser!!
                LocalUser.saveUser(appContext)
            }
            sus.resume(
                if (loadedUser.valid) Result.success(loadedUser!!) else Result.failure(
                    Throwable(it.exception)
                )
            )
        }
    }
}