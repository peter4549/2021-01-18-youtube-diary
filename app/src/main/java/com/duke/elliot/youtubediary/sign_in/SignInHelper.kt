package com.duke.elliot.youtubediary.sign_in

import com.duke.elliot.youtubediary.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import timber.log.Timber
import java.lang.Exception

class SignInHelper(private val signInActivity: SignInActivity) {

    private var onSignInListener: OnSignInListener? = null
    fun setOnSignInListener(onSignInListener: OnSignInListener) {
        this.onSignInListener = onSignInListener
    }

    @Suppress("SameParameterValue")
    private fun getString(resId: Int) = signInActivity.getString(resId)

    interface OnSignInListener {
        fun onAfterGoogleSignIn(result: Boolean)
        fun onBeforeGoogleSignIn()
    }

    fun signInWithGoogle() {
        onSignInListener?.onBeforeGoogleSignIn()

        try {
            val googleSignInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            val googleSignInClient = GoogleSignIn.getClient(signInActivity, googleSignInOptions)
            val signInIntent = googleSignInClient?.signInIntent

            signInActivity.startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE_SIGN_IN)
        } catch (e: Exception) {
            Timber.e(e)
            onSignInListener?.onAfterGoogleSignIn(false)
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.d("Google sign in successful.")
                onSignInListener?.onAfterGoogleSignIn(true)
            } else {
                Timber.d("Google sign in failed.")
                onSignInListener?.onAfterGoogleSignIn(false)
            }
        }
    }

    companion object {
        const val REQUEST_CODE_GOOGLE_SIGN_IN = 1559
    }
}