package com.apppause

import android.content.Context

/** Small wrapper around SharedPreferences for everything the app remembers. */
object Prefs {
    private const val FILE = "apppause"
    private const val KEY_GATED = "gated_apps"
    private const val KEY_SECONDS = "countdown_seconds"
    private const val KEY_GRACE_MIN = "grace_minutes"

    const val DEFAULT_SECONDS = 60
    const val DEFAULT_GRACE_MINUTES = 5

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---- which apps get the countdown ----

    fun gatedApps(c: Context): Set<String> =
        HashSet(sp(c).getStringSet(KEY_GATED, emptySet()) ?: emptySet())

    fun setGated(c: Context, pkg: String, on: Boolean) {
        val set = gatedApps(c).toMutableSet()
        if (on) set.add(pkg) else set.remove(pkg)
        sp(c).edit().putStringSet(KEY_GATED, set).apply()
    }

    fun setAllGated(c: Context, pkgs: Set<String>) {
        sp(c).edit().putStringSet(KEY_GATED, pkgs).apply()
    }

    // ---- settings ----

    fun seconds(c: Context): Int = sp(c).getInt(KEY_SECONDS, DEFAULT_SECONDS)

    fun setSeconds(c: Context, value: Int) {
        sp(c).edit().putInt(KEY_SECONDS, value.coerceIn(1, 600)).apply()
    }

    fun graceMinutes(c: Context): Int = sp(c).getInt(KEY_GRACE_MIN, DEFAULT_GRACE_MINUTES)

    fun setGraceMinutes(c: Context, value: Int) {
        sp(c).edit().putInt(KEY_GRACE_MIN, value.coerceIn(0, 240)).apply()
    }

    // ---- grace period: after you wait once, the app stays open-able for a while ----

    fun unlockUntil(c: Context, pkg: String): Long = sp(c).getLong("until_$pkg", 0L)

    fun grantGrace(c: Context, pkg: String) {
        val until = System.currentTimeMillis() + graceMinutes(c) * 60_000L
        sp(c).edit().putLong("until_$pkg", until).apply()
    }
}
