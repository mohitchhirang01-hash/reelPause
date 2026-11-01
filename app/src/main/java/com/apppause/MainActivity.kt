package com.apppause

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged

/** Setup screen: permissions, timer settings, and the list of apps to gate. */
class MainActivity : AppCompatActivity() {

    private data class AppItem(val pkg: String, val label: String)

    private lateinit var statusView: TextView
    private lateinit var accessibilityButton: Button
    private lateinit var overlayButton: Button
    private lateinit var listView: ListView
    private var apps: List<AppItem> = emptyList()
    private val selected = mutableSetOf<String>()

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected.addAll(Prefs.gatedApps(this))
        apps = loadApps()
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun loadApps(): List<AppItem> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherIntent, 0)
            .map { AppItem(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
            .distinctBy { it.pkg }
            .filter { it.pkg != packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val component = ComponentName(this, GateService::class.java)
        return enabled.contains(component.flattenToString()) ||
            enabled.contains(component.flattenToShortString())
    }

    private fun refreshStatus() {
        val a11y = isAccessibilityEnabled()
        val overlay = Settings.canDrawOverlays(this)
        statusView.text = buildString {
            append(if (a11y) "✓ Accessibility service is on\n" else "✗ Accessibility service is off\n")
            append(if (overlay) "✓ Display over other apps is allowed" else "✗ Display over other apps is not allowed")
        }
        accessibilityButton.visibility = if (a11y) View.GONE else View.VISIBLE
        overlayButton.visibility = if (overlay) View.GONE else View.VISIBLE
    }

    private fun numberField(initial: Int, onChange: (Int) -> Unit) = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER
        setText(initial.toString())
        minEms = 4
        doAfterTextChanged { text -> text?.toString()?.toIntOrNull()?.let(onChange) }
    }

    private fun labeledRow(label: String, field: EditText) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(
            TextView(context).apply { text = label; textSize = 16f },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        addView(field)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
        }

        root.addView(TextView(this).apply {
            text = "AppPause"
            textSize = 26f
        })
        root.addView(TextView(this).apply {
            text = "Waits before opening the apps you pick."
            textSize = 14f
        })

        statusView = TextView(this).apply { setPadding(0, 12.dp(), 0, 4.dp()) }
        root.addView(statusView)

        accessibilityButton = Button(this).apply {
            text = "Turn on accessibility service"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        overlayButton = Button(this).apply {
            text = "Allow display over other apps"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
        root.addView(accessibilityButton)
        root.addView(overlayButton)

        root.addView(labeledRow("Countdown (seconds)", numberField(Prefs.seconds(this)) {
            Prefs.setSeconds(this, it)
        }))
        root.addView(labeledRow("Skip repeat for (minutes)", numberField(Prefs.graceMinutes(this)) {
            Prefs.setGraceMinutes(this, it)
        }))

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(Button(this).apply {
            text = "Select all"
            setOnClickListener {
                selected.clear()
                selected.addAll(apps.map { it.pkg })
                Prefs.setAllGated(this@MainActivity, selected.toSet())
                (listView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        buttons.addView(Button(this).apply {
            text = "Select none"
            setOnClickListener {
                selected.clear()
                Prefs.setAllGated(this@MainActivity, emptySet())
                (listView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(buttons)

        listView = ListView(this)
        listView.adapter = object : ArrayAdapter<AppItem>(this, 0, apps) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = getItem(position)!!
                val box = (convertView as? CheckBox) ?: CheckBox(context).apply {
                    compoundDrawablePadding = 12.dp()
                    setPadding(4.dp(), 8.dp(), 4.dp(), 8.dp())
                }
                box.setOnCheckedChangeListener(null)
                box.text = item.label
                box.isChecked = item.pkg in selected
                try {
                    val icon = packageManager.getApplicationIcon(item.pkg)
                    icon.setBounds(0, 0, 40.dp(), 40.dp())
                    box.setCompoundDrawablesRelative(icon, null, null, null)
                } catch (e: Exception) {
                    box.setCompoundDrawablesRelative(null, null, null, null)
                }
                box.setOnCheckedChangeListener { _, checked ->
                    if (checked) selected.add(item.pkg) else selected.remove(item.pkg)
                    Prefs.setGated(this@MainActivity, item.pkg, checked)
                }
                return box
            }
        }
        root.addView(
            listView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        setContentView(root)
    }
}
