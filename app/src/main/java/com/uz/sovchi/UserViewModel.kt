package com.uz.sovchi

import androidx.lifecycle.ViewModel
import com.uz.sovchi.data.User
import com.uz.sovchi.data.UserRepository

class UserViewModel : ViewModel() {

    private val repository = UserRepository(appContext)

    suspend fun authFirebaseUser(): User? {
        return repository.authFirebaseUser()
    }

    suspend fun updateUser(user: User) {
        repository.updateUser(user)
    }

    fun signOut() = repository.signOut()

    val user: User get() = repository.user
}