package com.duke.elliot.youtubediary.base

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.main.MainApplication
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class BaseActivity: AppCompatActivity() {

    private var menuRes: Int? = null
    private var onHomePressed: (() -> Unit)? = null
    private var optionsItemIdAndOnSelectedListeners = mutableMapOf<Int, () -> Unit>()
    private var progressDialog: AlertDialog? = null
    private var showHomeAsUp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = MainApplication.primaryThemeColor
    }

    protected fun setOnHomePressedCallback(onHomePressed: () -> Unit) {
        this.onHomePressed = onHomePressed
    }

    protected fun setDisplayHomeAsUpEnabled(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        showHomeAsUp = true
    }

    @Suppress("SameParameterValue")
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

    protected fun actionBarHeight(): Int {
        val typedValue = TypedValue()
        var actionBarHeight = 0

        if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true))
            actionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)

        return actionBarHeight
    }

    protected fun showMaterialAlertDialog(
        title: String?,
        message: String?,
        neutralButtonText: String?,
        neutralButtonClickListener: ((DialogInterface?, Int) -> Unit)?,
        negativeButtonText: String?,
        negativeButtonClickListener: ((DialogInterface?, Int) -> Unit)?,
        positiveButtonText: String?,
        positiveButtonClickListener: ((DialogInterface?, Int) -> Unit)?
    ) {
        val materialAlertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setNeutralButton(neutralButtonText, neutralButtonClickListener)
            .setNegativeButton(negativeButtonText, negativeButtonClickListener)
            .setPositiveButton(positiveButtonText, positiveButtonClickListener)
            .setCancelable(false)
            .show()

        val textMessage = materialAlertDialog.findViewById<TextView>(android.R.id.message)
        val button1 = materialAlertDialog.findViewById<Button>(android.R.id.button1)
        val button2 = materialAlertDialog.findViewById<Button>(android.R.id.button2)
        val button3 = materialAlertDialog.findViewById<Button>(android.R.id.button3)

        @Suppress("DEPRECATION")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            textMessage?.setTextAppearance(R.style.WellTodayMediumFontFamilyStyle)
            button1?.setTextAppearance(R.style.WellTodayMediumFontFamilyStyle)
            button2?.setTextAppearance(R.style.WellTodayMediumFontFamilyStyle)
            button3?.setTextAppearance(R.style.WellTodayMediumFontFamilyStyle)
        }
        else {
            textMessage?.setTextAppearance(this, R.style.WellTodayMediumFontFamilyStyle)
            button1?.setTextAppearance(this, R.style.WellTodayMediumFontFamilyStyle)
            button2?.setTextAppearance(this, R.style.WellTodayMediumFontFamilyStyle)
            button3?.setTextAppearance(this, R.style.WellTodayMediumFontFamilyStyle)
        }
    }

    private fun MutableSet<Int>.notContains(element: Int) = !contains(element)
}