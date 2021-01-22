package com.duke.elliot.youtubediary.fluid_keyboard_resize

data class KeyboardVisibilityChanged(
    val visible: Boolean,
    val contentHeight: Int,
    val contentHeightBeforeResize: Int
)