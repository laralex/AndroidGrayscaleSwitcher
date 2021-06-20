package com.laralex.grayswitcher

import android.Manifest
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


// DONE: fully no UI to switch (even without flashing black screen)
// DONE: application icon
// DONE: check permission granted - show dialog with ADB command if not
// TODO: localization
// TODO: permission tip to better help with resolution
// TODO: make a button in status bar to switch aka switch Wi-fi or airplane mode
// TODO: better style of permission tip dialog (colors, highlights, fonts, remove top bar)
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
     * Activity entry point, after which resources and OS actions are accessible
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TAG = getString(R.string.app_tag)

        val hasWriteSettingsPermission = checkPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)
        if (!hasWriteSettingsPermission) {
            showPermissionTip()
            return
        }

        val previousGrayscaleValue = isGrayscaleEnabled()
        val newGrayscaleValue = setGrayscale(!previousGrayscaleValue)
        reportGrayscaleSwitch(previousGrayscaleValue, newGrayscaleValue, 500)
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
     * Checks whether a given permission is granted to this application
     *
     * @param permission code of a permission, preferably taken from Manifest.permission class
     * @return "true" if the permission is granted to this application, "false" otherwise
     */
    private fun checkPermissionGranted(permission: String): Boolean {
        val result = ContextCompat.checkSelfPermission(this, permission)
        return result == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Shows a UI dialog with possible solutions to acquire necessary permissions for the app
     */
    private fun showPermissionTip() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.permission_tip)

        setupClickableHyperlinks(dialog.findViewById(R.id.tipIntro))
        setupClickableHyperlinks(dialog.findViewById(R.id.tipSolution))
        setupClickableHyperlinks(dialog.findViewById(R.id.tipGuarantees))
        
        dialog.setOnDismissListener {
            finish()
        }
        val closeButton = dialog.findViewById<Button>(R.id.closeButton)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        val copyButton = dialog.findViewById<Button>(R.id.copyButton)
        copyButton.setOnClickListener {
            copyTextToClipboard(
                getString(R.string.adb_label),
                getString(R.string.adb_permission_command))
            Toast.makeText(this, getString(R.string.adb_command_copied), Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    /**
     * Adds the given text to system's clipboard
     *
     * @param label - user-seen label (small and descriptive)
     * @param text - content of clipboard
     */
    private fun copyTextToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Displays a message about switch of grayscale mode. Currently reports in Toast UI element and in DEBUG logs
     *
     * @param previousValue - whether grayscale mode had been enabled before switch was executed
     * @param newValue - whether grayscale mode is now enabled after switch was executed
     * @param durationMs - how many milliseconds the Toast message will be shown
     */
    private fun reportGrayscaleSwitch(previousValue: Boolean, newValue: Boolean, durationMs: Long) {
        val messageTemplate = if (previousValue != newValue) {
            getString(R.string.change_success_template)
        } else {
            getString(R.string.change_failure_template)
        }
        val updateValue = if (newValue) getString(R.string.mode_on) else getString(R.string.mode_off)
        val updateMessage = String.format(messageTemplate, updateValue)
        val toast = Toast.makeText(this, updateMessage, Toast.LENGTH_SHORT)

        toast.show()
        Log.d(TAG, updateMessage)

        Handler(Looper.getMainLooper())
            .postDelayed( { toast.cancel() }, durationMs)
    }

    /**
     * Enables clickable hyperlinks for the given TextView (because by default <a href...> links are not clickable)
     * Needed in permission tip dialog
     * @param txtView - destination TextView, which requires enabling of clickable hyperlinks
     */
    private fun setupClickableHyperlinks(txtView: TextView) {
        txtView.movementMethod = LinkMovementMethod.getInstance()
    }
}