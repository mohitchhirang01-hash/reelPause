package com.apppause

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.ceil

/** Full-screen countdown shown before a gated app opens. */
class TimerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "target_package"
    }

    private var target = ""
    private var appLabel = ""
    private var timer: CountDownTimer? = null
    private var opening = false

    private lateinit var titleView: TextView
    private lateinit var countView: TextView
    private lateinit var hintView: TextView
    private lateinit var openButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()

        // Back = "never mind": leave to the home screen instead of the gated app.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        begin(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        opening = false
        begin(intent)
    }

    // If the user leaves this screen mid-countdown, the wait is void.
    override fun onStop() {
        super.onStop()
        timer?.cancel()
        if (!opening) finish()
    }

    private fun begin(i: Intent) {
        target = i.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        appLabel = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(target, 0)).toString()
        } catch (e: Exception) {
            target
        }

        titleView.text = "Opening $appLabel"
        hintView.text = "Take a breath. Do you really need to open it?"
        openButton.text = "Open $appLabel"
        openButton.isEnabled = false

        val seconds = Prefs.seconds(this)
        countView.text = seconds.toString()

        timer?.cancel()
        timer = object : CountDownTimer(seconds * 1000L, 200L) {
            override fun onTick(msLeft: Long) {
                countView.text = ceil(msLeft / 1000.0).toInt().toString()
            }

            override fun onFinish() {
                countView.text = "0"
                hintView.text = "Okay, you waited. Your call."
                openButton.isEnabled = true
            }
        }.start()
    }

    private fun openTarget() {
        Prefs.grantGrace(this, target)
        val launch = packageManager.getLaunchIntentForPackage(target)
        opening = true
        if (launch != null) startActivity(launch)
        finish()
    }

    private fun goHome() {
        opening = false
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private fun buildUi() {
        val pad = (24 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#101418"))
            setPadding(pad, pad, pad, pad)
        }

        titleView = TextView(this).apply {
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        countView = TextView(this).apply {
            textSize = 96f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        hintView = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#9AA5B1"))
            gravity = Gravity.CENTER
        }
        openButton = Button(this).apply {
            isEnabled = false
            setOnClickListener { openTarget() }
        }
        val cancelButton = Button(this).apply {
            text = "Never mind"
            setOnClickListener { goHome() }
        }

        val wrap = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = pad }

        root.addView(titleView, wrap)
        root.addView(countView, wrap)
        root.addView(hintView, wrap)
        root.addView(openButton, wrap)
        root.addView(cancelButton, wrap)

        setContentView(root)
    }
}
