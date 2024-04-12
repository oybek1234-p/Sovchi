package com.uz.sovchi.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserRepository(val context: Context) {

    init {
        LocalUser.getUser(context)
    }

    private val usersReference: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("users")
    }

    val user: User get() = LocalUser.user
    private var userLoading = false

    private fun saveUserNetwork() {
        if (user.valid) {
            usersReference.child(user.uid).setValue(user)
        }
    }

    fun loadAllUsers(done: (list: List<User>) -> Unit) {
        usersReference.get().addOnCompleteListener { it ->
            done.invoke(it.result.children.mapNotNull { it.getValue(User::class.java) })
        }
    }

    fun setUnreadZero() {
        if (user.valid) {
            if (user.unreadMessages == 0) return
            usersReference.child(user.uid).child(User::unreadMessages.name).setValue(0)
        }
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
        FirebaseAuth.getInstance().signOut()
        LocalUser.user = User()
        LocalUser.saveUser(context)
    }

    private fun createNewUserAndSet(user: User) {
        if (user.valid) {
            LocalUser.user = user
            LocalUser.saveUser(context)

            saveUserNetwork()
        }
    }

    fun updateUser(user: User) {
        if (user.valid && user.uid == LocalUser.user.uid) {
            LocalUser.user = user
            LocalUser.saveUser(context)

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
                uid = id, "", firebaseUser.phoneNumber ?: "", System.currentTimeMillis(), false, 0
            )
            createNewUserAndSet(newUser)
            newUser
        } else {
            LocalUser.user = networkUser!!
            LocalUser.saveUser(context)
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
                LocalUser.saveUser(context)
            }
            sus.resume(
                if (loadedUser.valid) Result.success(loadedUser!!) else Result.failure(
                    Throwable(it.exception)
                )
            )
        }
    }
}