package com.laralex.grayswitcher

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast

// DONE: fully no UI (even without flashing black screen)
// DONE: application icon
// TODO: localization
// TODO: check permission granted - show dialog with ADB command if not
// TODO: make settings screen (choose daltonizer mode, choose screen saturation)
// TODO: make gesture (globally detect), might need to turn the app into a background service
class SwitcherActivity : AppCompatActivity() {
    companion object {
        lateinit var TAG: String
    }

    /**
     * Represents all possible variants of "Developer options -> Simulate color space" setting
     *
     * @property code - numerical code of a variant (needed to pass it in Settings.Secure.putString method)
     */
    private enum class DaltonizerMode(val code: Int) {
        DISABLED(-1),
        MONOCHROMACY(0),
        PROTANOMALY(11),
        CORRECT_DEUTERANOMALY(12),
        TRITANOMALY(13),
    }

    /**
     * Activity entry point, after which resources and OS actions are accesible
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TAG = getString(R.string.app_tag)
        Log.d(TAG, "onCreate")
        val previousGrayscaleValue = isGrayscaleEnabled()
        val newGrayscaleValue = setGrayscale(!previousGrayscaleValue)
        reportGrayscaleSwitch(previousGrayscaleValue, newGrayscaleValue)
        finish()
    }

    /**
     * Tries to set the "Developer option" for simulation of screen color space
     *
     * @param shouldTurnOn - pass "true" to try to enable grayscale mode, pass "false" to try to disable
     * @return "true" when now grayscale mode is enabled, "false" otherwise
     */
    private fun setGrayscale(shouldTurnOn: Boolean): Boolean {
        val daltonizerEnabledString = if (shouldTurnOn) "1" else "0"
        val daltonizerModeString = if (shouldTurnOn) {
            DaltonizerMode.MONOCHROMACY.code.toString()
        } else {
            DaltonizerMode.DISABLED.code.toString()
        }
        Settings.Secure.putString(this.contentResolver, "accessibility_display_daltonizer_enabled", daltonizerEnabledString)
        Settings.Secure.putString(this.contentResolver, "accessibility_display_daltonizer", daltonizerModeString)
        return isGrayscaleEnabled()
    }

    /**
     * Checks whether the "Developer option" for screen color space is set to "Monochrome"
     *
     * @return "true" when screen color space is set to "Monochrome", "false" otherwise (including "Disabled")
     */
    private fun isGrayscaleEnabled(): Boolean {
        val daltonizerEnabledString = Settings.Secure.getString(this.contentResolver, "accessibility_display_daltonizer_enabled")
        val daltonizerModeString = Settings.Secure.getString(this.contentResolver, "accessibility_display_daltonizer")
        return daltonizerEnabledString == "1" && daltonizerModeString == DaltonizerMode.MONOCHROMACY.code.toString()
    }

    /**
     * Displays a message about switch of grayscale mode. Currently reports in Toast UI element and in DEBUG logs
     *
     * @param previousValue - whether grayscale mode had been enabled before switch was executed
     * @param newValue - whether grayscale mode is now enabled after switch was executed
     */
    private fun reportGrayscaleSwitch(previousValue: Boolean, newValue: Boolean) {
        val messageTemplate = getString(R.string.update_message_template)
        val updateMessage = String.format(messageTemplate,
            if (previousValue) "ON" else "OFF",
            if (newValue) "ON" else "OFF")
        Toast.makeText(this, updateMessage, Toast.LENGTH_SHORT).show()
        Log.d(TAG, updateMessage)
    }
}