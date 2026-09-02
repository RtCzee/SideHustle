package com.example.sidehustle.util

import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object FirebaseTokenProvider {

    suspend fun getIdToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null

        return suspendCoroutine { continuation ->
            user.getIdToken(false)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(task.result?.token)
                    } else {
                        continuation.resume(null)
                    }
                }
        }
    }
}
