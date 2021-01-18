package com.duke.elliot.youtubediary.base

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.duke.elliot.youtubediary.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class BaseActivity: AppCompatActivity() {

    private var menuRes: Int? = null
    private var onHomePressed: (() -> Unit)? = null
    private var optionsItemIdAndOnSelectedListeners = mutableMapOf<Int, () -> Unit>()
    private var progressDialog: AlertDialog? = null
    private var showHomeAsUp = false

    protected fun setOnHomePressedCallback(onHomePressed: () -> Unit) {
        this.onHomePressed = onHomePressed
    }

    protected fun setDisplayHomeAsUpEnabled(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        showHomeAsUp = true
    }

    protected fun setOptionsMenu(menuRes: Int?) {
        this.menuRes = menuRes
    }

    protected fun setOnOptionsItemSelectedListeners(vararg optionsItemIdAndOnSelectedListeners: Pair<Int, () -> Unit>) {
        optionsItemIdAndOnSelectedListeners.forEach {
            if (this.optionsItemIdAndOnSelectedListeners.keys.notContains(it.first))
                this.optionsItemIdAndOnSelectedListeners[it.first] = it.second
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.clear()
        menuRes?.let { menuInflater.inflate(it, menu) }
        return menuRes != null || showHomeAsUp
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onHomePressed?.invoke()
                true
            }
            else -> {
                optionsItemIdAndOnSelectedListeners[item.itemId]?.invoke()
                true
            }
        }
    }

    protected fun setBackgroundColor(vararg views: View, @ColorInt color: Int) {
        for (view in views) {
            view.setBackgroundColor(color)
            view.invalidate()
        }
    }

    fun showToast(text: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(this, text, duration).show()
    }

    protected fun showProgressDialog() {
        progressDialog?.show() ?: run {
            val builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setView(R.layout.progress_dialog)

            val alertDialog = builder.create()
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            progressDialog = alertDialog
            progressDialog?.show()
        }
    }

    protected fun dismissProgressDialog() {
        progressDialog?.dismiss()
    }

    protected fun showAlertDialog(
        title: String?,
        message: String?,
        neutralButtonText: String?,
        neutralButtonClickListener: ((DialogInterface?, Int) -> Unit)?,
        negativeButtonText: String?,
        negativeButtonClickListener: ((DialogInterface?, Int) -> Unit)?,
        positiveButtonText: String?,
        positiveButtonClickListener: ((DialogInterface?, Int) -> Unit)?
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setNeutralButton(neutralButtonText, neutralButtonClickListener)
            .setNegativeButton(negativeButtonText, negativeButtonClickListener)
            .setPositiveButton(positiveButtonText, positiveButtonClickListener)
            .show()
    }

    private fun MutableSet<Int>.notContains(element: Int) = !contains(element)
}