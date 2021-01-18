package com.duke.elliot.youtubediary.sign_in

import android.content.Intent
import android.os.Bundle
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.base.BaseActivity
import com.duke.elliot.youtubediary.sign_in.SignInHelper.Companion.REQUEST_CODE_GOOGLE_SIGN_IN
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.android.synthetic.main.activity_sign_in.*
import timber.log.Timber

class SignInActivity: BaseActivity(), SignInHelper.OnSignInListener {

    private lateinit var signInHelper: SignInHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        signInHelper = SignInHelper(this)
        signInHelper.setOnSignInListener(this)

        text_signInWithGoogle.setOnClickListener {
            signInHelper.signInWithGoogle()
        }
    }

    override fun onAfterGoogleSignIn(result: Boolean) {
        dismissProgressDialog()

        val intent = Intent()

        if (result) {
            showToast(getString(R.string.sign_in_success_message))
            setResult(RESULT_OK, intent)
            finish()
        } else {
            showToast(getString(R.string.failed_to_sign_in_with_google))
            setResult(RESULT_CANCELED, intent)
            finish()
        }
    }

    override fun onBeforeGoogleSignIn() {
        showProgressDialog()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_GOOGLE_SIGN_IN -> {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(ApiException::class.java) // 여기서 에러,
                    signInHelper.firebaseAuthWithGoogle(account)
                } catch (e: ApiException) {
                    Timber.e(e, getString(R.string.failed_to_sign_in_with_google))
                    showToast("${getString(R.string.failed_to_sign_in_with_google)}: ${e.message}")

                    val intent = Intent()
                    setResult(RESULT_CANCELED, intent)
                    finish()
                }
            }
        }
    }
}