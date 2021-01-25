package com.duke.elliot.youtubediary.drawer

import android.app.Activity
import android.content.Context
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.core.content.ContextCompat
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.main.MainApplication
import com.duke.elliot.youtubediary.util.SimpleDialogFragment
import com.duke.elliot.youtubediary.util.SimpleItem
import com.duke.elliot.youtubediary.util.toHexColor
import petrov.kristiyan.colorpicker.ColorPicker
import java.lang.IllegalArgumentException

const val PREFERENCES_THEME = "com.duke.elliot.youtubediary.drawer" +
        ".drawer_menu_util.preferences_theme"
const val KEY_PRIMARY_THEME_COLOR = "com.duke.elliot.youtubediary.drawer" +
        ".drawer_menu_util.key_primary_theme_color"
private const val KEY_NIGHT_MODE = "com.duke.elliot.youtubediary.drawer" +
        ".theme_util.key_night_mode"

object DrawerMenuUtil {

    /** Theme Color */
    fun showColorPicker(activity: Activity, onChooseCallback: (color: Int) -> Unit) {
        val themeColors = activity.resources.getIntArray(R.array.theme_colors).toList()
        val hexColors = themeColors.map { it.toHexColor() } as ArrayList

        val colorPicker = ColorPicker(activity)
        colorPicker.setOnChooseColorListener(object : ColorPicker.OnChooseColorListener {
            override fun onChooseColor(position: Int, color: Int) {
                if (color != 0) {
                    MainApplication.primaryThemeColor = color
                    storeThemeColor(activity, MainApplication.primaryThemeColor)
                    onChooseCallback.invoke(MainApplication.primaryThemeColor)
                }
            }

            override fun onCancel() {}
        })
            .setTitle(activity.getString(R.string.theme_color))
            .setColumns(6)
            .setColorButtonMargin(2, 2, 2, 2)
            .setColorButtonDrawable(R.drawable.background_white_rounded_corners)
            .setColors(hexColors)
            .setDefaultColorButton(MainApplication.primaryThemeColor)
            .show()

        colorPicker.positiveButton.text = activity.getString(R.string.ok)
        colorPicker.negativeButton.text = activity.getString(R.string.cancel)
    }

    private fun storeThemeColor(context: Context, @ColorInt color: Int) {
        val preferences = context.getSharedPreferences(PREFERENCES_THEME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putInt(KEY_PRIMARY_THEME_COLOR, color)
        editor.apply()
    }

    @ColorInt
    fun restoreThemeColor(context: Context): Int {
        val preferences = context.getSharedPreferences(PREFERENCES_THEME, Context.MODE_PRIVATE)
        return preferences.getInt(
            KEY_PRIMARY_THEME_COLOR, ContextCompat.getColor(
                context,
                R.color.default_primary_theme
            )
        )
    }

    private fun storeNightMode(context: Context, nightMode: Int) {
        val preferences = context.getSharedPreferences(PREFERENCES_THEME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putInt(KEY_NIGHT_MODE, nightMode)
        editor.apply()
    }

    fun restoreNightMode(context: Context): Int {
        val preferences = context.getSharedPreferences(PREFERENCES_THEME, Context.MODE_PRIVATE)
        return preferences.getInt(KEY_NIGHT_MODE, MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /** Night Mode */
    fun getNightModePicker(
        context: Context,
        onNightModeSelect: (nightMode: Int, nightModeString: String) -> Unit
    ): SimpleDialogFragment {
        val darkTheme = context.getString(R.string.dark_theme)
        val lightTheme = context.getString(R.string.light_theme)
        val systemDefaultTheme = context.getString(R.string.system_default_theme)

        val themes = arrayOf(
            darkTheme,
            lightTheme,
            systemDefaultTheme
        )

        val simpleListDialogFragment = SimpleDialogFragment()
        simpleListDialogFragment.setTitle(context.getString(R.string.night_mode))
        simpleListDialogFragment.setItems(
            themes.map { SimpleItem( it, it, null ) } as ArrayList<SimpleItem>
        )
        simpleListDialogFragment.setOnItemSelectedListener { dialogFragment, simpleItem ->
            val nightMode = when(simpleItem.name) {
                darkTheme -> MODE_NIGHT_YES
                lightTheme -> MODE_NIGHT_NO
                systemDefaultTheme -> MODE_NIGHT_FOLLOW_SYSTEM
                else -> throw IllegalArgumentException("Invalid night mode.")
            }

            storeNightMode(context, nightMode)
            onNightModeSelect.invoke(nightMode, simpleItem.name)
            dialogFragment.dismiss()
        }

        return simpleListDialogFragment
    }
}