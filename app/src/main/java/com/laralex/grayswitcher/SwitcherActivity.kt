package com.laralex.grayswitcher

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast

// TODO: fully no UI (even without flashing black screen)
// TODO: application icon
// TODO: localization
// TODO: check permission granted - show dialog with ADB command if not
class SwitcherActivity : AppCompatActivity() {
    companion object {
        lateinit var TAG: String
    }
    private enum class DaltonizerMode(val code: Int) {
        DISABLED(-1),
        MONOCHROMACY(0),
        PROTANOMALY(11),
        CORRECT_DEUTERANOMALY(12),
        TRITANOMALY(13),
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TAG = getString(R.string.app_tag)
        Log.d(TAG, "onCreate")
        val previousGrayscaleValue = isGrayscaleEnabled()
        val newGrayscaleValue = setGrayscale(!previousGrayscaleValue)
        reportGrayscaleSwitch(previousGrayscaleValue, newGrayscaleValue)
        finish()
    }

    // Returns new value of grayscale setting
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

    private fun isGrayscaleEnabled(): Boolean {
        val daltonizerEnabledString = Settings.Secure.getString(this.contentResolver, "accessibility_display_daltonizer_enabled")
        val daltonizerModeString = Settings.Secure.getString(this.contentResolver, "accessibility_display_daltonizer")
        return daltonizerEnabledString == "1" && daltonizerModeString == DaltonizerMode.MONOCHROMACY.code.toString()
    }

    private fun reportGrayscaleSwitch(previousValue: Boolean, newValue: Boolean) {
        val messageTemplate = getString(R.string.update_message_template)
        val updateMessage = String.format(messageTemplate,
            if (previousValue) "ON" else "OFF",
            if (newValue) "ON" else "OFF")
        Toast.makeText(this, updateMessage, Toast.LENGTH_SHORT).show()
        Log.d(TAG, updateMessage)
    }
}