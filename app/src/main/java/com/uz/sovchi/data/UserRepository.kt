package com.uz.sovchi.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
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

    suspend fun updateUser(user: User) {
        if (user.valid && user.uid == LocalUser.user.uid) {
            LocalUser.user = user
            LocalUser.saveUser(context)

            saveUserNetwork()
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
            val newUser =
                User(uid = id, "", firebaseUser.phoneNumber ?: "", System.currentTimeMillis())
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