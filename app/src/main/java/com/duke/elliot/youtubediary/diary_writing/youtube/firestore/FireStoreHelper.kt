package com.duke.elliot.youtubediary.diary_writing.youtube.firestore

import com.duke.elliot.youtubediary.database.youtube.DisplayChannelModel
import com.duke.elliot.youtubediary.main.MainApplication
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import org.json.JSONObject
import timber.log.Timber

class FireStoreHelper {

    private val firebaseUser = MainApplication.getFirebaseAuthInstance().currentUser
    private val userCollectionReference = FirebaseFirestore.getInstance().collection(COLLECTION_USERS)
    private val gson = Gson()
    private var onDocumentSnapshotListener: OnDocumentSnapshotListener? = null
    fun setOnDocumentSnapshotListener(onDocumentSnapshotListener: OnDocumentSnapshotListener) {
        this.onDocumentSnapshotListener = onDocumentSnapshotListener
    }

    interface OnDocumentSnapshotListener {
        fun onUserDocumentSnapshot(user: UserModel)
    }

    /** 로그인후 호출되어야함. user id로만 부를 것. */
    fun setUserSnapshotListener(uid: String, onFailure: (() -> Unit)? = null, onSuccess: (() -> Unit)? = null) {
        val documentReference = userCollectionReference.document(uid)
        documentReference.addSnapshotListener { documentSnapshot, fireStoreException ->
            fireStoreException?.let {
                // Exception occured.
                // 채널 정보를 불러올 수 없습니다. 못했습니다.. 등등.
                onFailure?.invoke()
            } ?: run {
                documentSnapshot?.let { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        documentSnapshot.data?.let {
                            val user =  gson.fromJson(JSONObject(it).toString(), UserModel::class.java)
                            onDocumentSnapshotListener?.onUserDocumentSnapshot(user)
                        }

                        /*
                        val channels = documentSnapshot.data
                                ?.get(UserModel.FILED_YOUTUBE_CHANNELS) as? MutableList<*>
                        // 이거를 서브밋 하면된다.
                        val cc = documentSnapshot.data?.getValue(UserModel.FILED_YOUTUBE_CHANNELS) as? List<*>

                        if (channels != null && channels.isNotEmpty()) {
                            val parsedChannels = channels.map { gson.fromJson(JSONObject(it.toString()).toString(), ChannelModel::class.java) }

                            if (activity is YouTubeChannelsActivity)
                                activity.submitChannels(parsedChannels)
                        }

                         */
                    } else {
                        setUser()
                    }
                } ?: run {
                    onFailure?.invoke()
                }
            }
        }
    }

    private fun setUser() {
        firebaseUser?.uid?.let {
            val user = UserModel(
                    uid = it,
                    premium = false
            )

            val documentReference = userCollectionReference.document(user.uid)
            documentReference.set(user).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.d("User set.")
                    // 유저 정보를 생성함.
                } else {
                    task.exception?.let { e ->
                        Timber.e(e, "Failed to set user.")
                        // 유저 정보를 생성 못함.
                    }
                }
            }
        } ?: run {
            // Same as login failed.
            // 로그인 실패 상태와 동일. 이게 불렸다면,, 찰나의 순간에 로그아웃됫다는 것. 말이 안되긴함.
            // 메시지: 유저 정보를 생성하지 못했습니다.
        }
    }

    fun addChannel(channel: DisplayChannelModel) {
        firebaseUser?.uid?.let {
            val documentReference = userCollectionReference.document(it)
            documentReference.update(UserModel.FILED_YOUTUBE_CHANNELS, FieldValue.arrayUnion(channel))
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful)
                            Timber.d("youtubeChannelId updated.")
                        else
                            Timber.e("failed to update youtubeChannelId.")
                    }
        }
    }

    fun deleteChannel(channel: DisplayChannelModel) {
        firebaseUser?.uid?.let {
            val documentReference = userCollectionReference.document(it)
            documentReference.update(UserModel.FILED_YOUTUBE_CHANNELS, FieldValue.arrayRemove(channel))
                .addOnCompleteListener { task ->
                    if (task.isSuccessful)
                        Timber.d("youtubeChannelId deleted.")
                    else
                        Timber.e("Failed to delete youtubeChannelId.")
                }
        }
    }

    /*


        documentReference.set(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast(requireContext(), getString(R.string.profile_stored))
                MainActivity.currentUser = user
                requireActivity().onBackPressed()
            } else {
                task.exception?.let { e ->
                    errorHandler.errorHandling(e, getString(R.string.failed_to_store_profile))
                } ?: run {
                    errorHandler.errorHandling(
                        NullPointerException("failed to store user information, task.exception is null"),
                        getString(R.string.failed_to_store_profile))
                }
            }
        }
    }

    private fun storeUser(user: UserModel) {
        val documentReference = FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS).document(user.uid)

        val firebaseAuth = MainApplication.getFirebaseAuthInstance()

        firebaseAuth.currentUser?.let { user ->

        }

        if (MainActivity.currentUser != null) {
            MainActivity.currentUser?.categories = selectedCategories
            MainActivity.currentUser?.email = email
            MainActivity.currentUser?.publicName = publicName
            MainActivity.currentUser?.userType = selectedUserType

            documentReference.update(mapOf(
                UserModel.KEY_CATEGORIES to MainActivity.currentUser?.categories,
                UserModel.KEY_EMAIL to MainActivity.currentUser?.email,
                UserModel.KEY_PUBLIC_NAME to MainActivity.currentUser?.publicName,
                UserModel.KEY_USER_TYPE to MainActivity.currentUser?.userType
            )).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast(requireContext(), getString(R.string.profile_updated))
                    requireActivity().onBackPressed()
                } else {
                    task.exception?.let { e ->
                        errorHandler.errorHandling(e, getString(R.string.failed_to_store_profile))
                    } ?: run {
                        errorHandler.errorHandling(
                            NullPointerException("failed to store user information, task.exception is null"),
                            getString(R.string.failed_to_store_profile))
                    }
                }
            }
        }
        else {

        }
    }

     */

    companion object {
        private const val COLLECTION_USERS = "users"
    }
}