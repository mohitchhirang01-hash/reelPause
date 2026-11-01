package com.apppause

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * Watches which app comes to the foreground. When it is one of the apps the user
 * picked, and the grace period has run out, it shows the countdown screen first.
 */
class GateService : AccessibilityService() {

    private var lastPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Our own screens (countdown, settings): remember them as "the last thing on
        // screen" so that going back to a gated app from here counts as a new open.
        if (pkg == packageName) {
            lastPackage = pkg
            return
        }

        // Only react when the foreground app actually changes.
        if (pkg == lastPackage) return

        // Keyboards and system UI also produce window events; they are not "apps".
        if (pkg == "com.android.systemui" || pkg in inputMethodPackages()) return

        lastPackage = pkg

        if (pkg !in Prefs.gatedApps(this)) return
        if (System.currentTimeMillis() < Prefs.unlockUntil(this, pkg)) return

        val intent = Intent(this, TimerActivity::class.java)
            .putExtra(TimerActivity.EXTRA_PACKAGE, pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun inputMethodPackages(): Set<String> {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.map { it.packageName }.toSet()
    }

    override fun onInterrupt() {}
}
