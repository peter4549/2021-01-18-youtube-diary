package com.duke.elliot.youtubediary

import com.duke.elliot.youtubediary.diary_writing.youtube.ChannelModel
import com.duke.elliot.youtubediary.util.SimpleDialogFragment

/*
private fun chooseAccount() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        val intent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
        )
        startActivityForResult(intent, YouTubeChannelsActivity.REQUEST_CODE_CHOOSE_ACCOUNT)
    }
}

private fun getOAuth2(accountName: String?, accountType: String?) {
    accountName?.let {
        accountType?.let {
            val accountManager = AccountManager.get(this)
            accountManager.getAuthToken(
                    Account(accountName, accountType),
                    "ah",
                    null,
                    this,
                    OnTokenAcquired(),
                    null
            )
        }
    }
}

inner class OnTokenAcquired : AccountManagerCallback<Bundle> {
    override fun run(result: AccountManagerFuture<Bundle>) {
        try {
            val bundle: Bundle = result.result
            val authToken: String? = bundle.getString(AccountManager.KEY_AUTHTOKEN)

            coroutineScope.launch {
                val channelsDeferred = YouTubeApi.channelsService().getChannelsAsync("Bearer $authToken")
                try {
                    showToast(channelsDeferred.await().toString())
                } catch (t: Throwable) {
                    Timber.e(t, "Failed to get channel.")
                    showToast(t.message.toString())
                }
            }


        } catch (e: Exception) {
            Timber.e(e, "Failed to get channel.")
        }
    }
}
 */

/*
private fun showChannelSelectionDialog(channels: ArrayList<ChannelModel>) {
    val title = getString(R.string.channel)
    val items = channels.map {
        SimpleDialogFragment.Companion.Item(it.title, it.thumbnailUri)
    } as ArrayList
    SimpleDialogFragment().apply {
        setTitle(title)
        setItems(items)
        setOnItemSelectedListener { _, item ->
            val itemSelected = channels.filter {
                it.title == item.name && it.thumbnailUri == item.imageUri
            }

            if (itemSelected.isNotEmpty()) {
                val channelSelected = itemSelected[0]
                showToast(channelSelected.title)
            }
        }
    }.show(supportFragmentManager, null)
}
 */