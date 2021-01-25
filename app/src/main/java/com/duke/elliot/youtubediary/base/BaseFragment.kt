package com.duke.elliot.youtubediary.base

import android.content.Context
import android.content.DialogInterface
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.main.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

open class BaseFragment: Fragment() {

    private var menuRes: Int? = null
    private var onBackPressed: (() -> Unit)? = null
    private var onHomePressed: (() -> Unit)? = null
    private var optionsItemIdAndOnSelectedListeners = mutableMapOf<Int, () -> Unit>()

    protected fun setOnBackPressedCallback(onBackPressed: () -> Unit) {
        this.onBackPressed = onBackPressed
        val onBackPressedCallback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    this@BaseFragment.onBackPressed?.invoke()
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    protected fun setDisplayHomeAsUpEnabled(
        toolbar: Toolbar,
        @DrawableRes navigationIcon: Int? = null,
        onHomePressed: () -> Unit
    ) {
        (requireActivity() as MainActivity).setSupportActionBar(toolbar)
        (requireActivity() as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navigationIcon?.let { toolbar.setNavigationIcon(it) }
        setHasOptionsMenu(true)
        this.onHomePressed = onHomePressed
    }

    protected fun setOptionsMenu(toolbar: Toolbar, menuRes: Int?) {
        (requireActivity() as MainActivity).setSupportActionBar(toolbar)
        this.menuRes = menuRes
        setHasOptionsMenu(true)
    }

    protected fun setOnOptionsItemSelectedListeners(vararg optionsItemIdAndOnSelectedListeners: Pair<Int, () -> Unit>) {
        optionsItemIdAndOnSelectedListeners.forEach {
            if (this.optionsItemIdAndOnSelectedListeners.keys.notContains(it.first))
                this.optionsItemIdAndOnSelectedListeners[it.first] = it.second

            this.optionsItemIdAndOnSelectedListeners.keys.isEmpty()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        menuRes?.let {
            inflater.inflate(it, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> onHomePressed?.invoke()
            else -> optionsItemIdAndOnSelectedListeners[item.itemId]?.invoke()
        }

        return super.onOptionsItemSelected(item)
    }

    protected fun setBackgroundColor(vararg views: View, @ColorInt color: Int) {
        for (view in views) {
            view.setBackgroundColor(color)
            view.invalidate()
        }
    }

    protected fun showMaterialAlertDialog(
        context: Context,
        title: String?,
        message: String?,
        neutralButtonText: String?,
        neutralButtonClickListener: ((DialogInterface?, Int) -> Unit)?,
        negativeButtonText: String?,
        negativeButtonClickListener: ((DialogInterface?, Int) -> Unit)?,
        positiveButtonText: String?,
        positiveButtonClickListener: ((DialogInterface?, Int) -> Unit)?
    ) {
        val materialAlertDialog = MaterialAlertDialogBuilder(context)
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
            textMessage?.setTextAppearance(context, R.style.WellTodayMediumFontFamilyStyle)
            button1?.setTextAppearance(context, R.style.WellTodayMediumFontFamilyStyle)
            button2?.setTextAppearance(context, R.style.WellTodayMediumFontFamilyStyle)
            button3?.setTextAppearance(context, R.style.WellTodayMediumFontFamilyStyle)
        }
    }

    protected fun setToolbarFont(toolbar: Toolbar, resId: Int) {
        toolbar.setTitleTextAppearance(requireContext(), resId)
    }

    protected fun showToast(text: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(requireContext(), text, duration).show()
    }

    private fun MutableSet<Int>.notContains(element: Int) = !contains(element)
}

