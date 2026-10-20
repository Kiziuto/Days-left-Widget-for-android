package com.lifeindays.widget

import android.app.Activity
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Widget settings: date of birth + lifespan. Opens when the widget is added, when the widget is
 * tapped, and from the app icon. Shows a live preview rendered by the same code as the widget.
 */
class SettingsActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var dob: LocalDate = LifeStore.DEFAULT_DOB
    private var years: Int = LifeStore.DEFAULT_YEARS

    private lateinit var preview: ImageView
    private lateinit var dobText: TextView
    private lateinit var info: TextView
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out of the first-time setup must cancel adding the widget.
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        }
        dob = LifeStore.dob(this)
        years = LifeStore.years(this)
        setContentView(buildUi())
        refreshPreview()
    }

    // ---------------------------------------------------------------- UI

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(Neon.PAGE) }
        @Suppress("DEPRECATION")
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom
            )
            insets
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(24))
        }
        scroll.addView(col, ViewGroup.LayoutParams(MATCH, WRAP))

        col.addView(text("LIFE COUNTDOWN", 12f, Neon.CYAN, 600, spacing = 0.25f))
        col.addView(text("Set your timeline", 26f, Neon.ICE, 700).apply { setPadding(0, dp(6), 0, dp(16)) })

        preview = ImageView(this).apply { adjustViewBounds = true }
        col.addView(preview, LinearLayout.LayoutParams(MATCH, WRAP))

        col.addView(text("DATE OF BIRTH", 11f, Neon.withAlpha(Neon.ICE, 0x99), 600, spacing = 0.2f)
            .apply { setPadding(0, dp(22), 0, dp(8)) })
        dobText = text("", 17f, Neon.ICE, 600).apply {
            background = box()
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setOnClickListener { pickDate() }
        }
        col.addView(dobText, LinearLayout.LayoutParams(MATCH, WRAP))

        col.addView(text("LIFESPAN IN YEARS", 11f, Neon.withAlpha(Neon.ICE, 0x99), 600, spacing = 0.2f)
            .apply { setPadding(0, dp(18), 0, dp(8)) })
        val yearsField = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(years.toString())
            setTextColor(Neon.ICE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            Fonts.apply(this, 600)
            background = box()
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    s?.toString()?.trim()?.toIntOrNull()?.takeIf { it in MIN_YEARS..MAX_YEARS }?.let {
                        years = it
                        refreshPreview()
                    }
                }
            })
        }
        col.addView(yearsField, LinearLayout.LayoutParams(MATCH, WRAP))

        info = text("", 13f, Neon.withAlpha(Neon.ICE, 0xAA), 400).apply { setPadding(0, dp(10), 0, dp(20)) }
        col.addView(info)

        val save = text("SAVE", 16f, Neon.PAGE, 800, spacing = 0.2f).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Neon.CYAN)
            }
            setOnClickListener { save(yearsField.text.toString()) }
        }
        col.addView(save, LinearLayout.LayoutParams(MATCH, WRAP))

        // Standalone (opened from the app icon, no widget yet): offer to place the widget.
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            val add = text("ADD WIDGET TO HOME SCREEN", 13f, Neon.CYAN, 600, spacing = 0.15f).apply {
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(16), dp(16), dp(16))
                setOnClickListener { requestPin() }
            }
            col.addView(add, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
        }
        return scroll
    }

    private fun refreshPreview() {
        val m = LifeModel(dob, years)
        val dm = resources.displayMetrics
        val wPx = (dm.widthPixels - dp(40)).coerceAtLeast(200)
        val hPx = (wPx * 0.5f).toInt()
        preview.setImageBitmap(WidgetRenderer.render(this, m, wPx, hPx))
        dobText.text = dateFormat.format(dob)
        info.text = "${Fmt.int(m.total)} days in ${m.years} years, ${Fmt.int(m.remaining)} still ahead."
    }

    private fun pickDate() {
        DatePickerDialog(this, { _, y, mo, d ->
            dob = LocalDate.of(y, mo + 1, d)
            refreshPreview()
        }, dob.year, dob.monthValue - 1, dob.dayOfMonth).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun save(yearsInput: String) {
        val y = yearsInput.trim().toIntOrNull()
        when {
            y == null || y !in MIN_YEARS..MAX_YEARS ->
                Toast.makeText(this, "Enter a lifespan between $MIN_YEARS and $MAX_YEARS years.", Toast.LENGTH_SHORT).show()
            dob.isAfter(LocalDate.now()) ->
                Toast.makeText(this, "Date of birth can't be in the future.", Toast.LENGTH_SHORT).show()
            else -> {
                LifeStore.save(this, dob, y)
                // A configure screen must trigger the first draw itself, then report success.
                LifeDaysWidget.refreshAll(this)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                } else if (!LifeDaysWidget.hasWidgets(this)) {
                    Toast.makeText(this, "Saved. Add the widget from your home screen.", Toast.LENGTH_LONG).show()
                }
                finish()
            }
        }
    }

    private fun requestPin() {
        val manager = AppWidgetManager.getInstance(this)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(this, LifeDaysWidget::class.java), null, null)
        } else {
            Toast.makeText(this, "Long-press your home screen, choose Widgets, then Life in Days.", Toast.LENGTH_LONG).show()
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun box() = GradientDrawable().apply {
        cornerRadius = dp(12).toFloat()
        setColor(0xFF0B1220.toInt())
        setStroke(dp(1), Neon.withAlpha(Neon.CYAN, 0x66))
    }

    private fun text(s: CharSequence, sizeSp: Float, color: Int, weight: Int, spacing: Float = 0f) =
        TextView(this).apply {
            text = s
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(color)
            letterSpacing = spacing
            Fonts.apply(this, weight)
        }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val MIN_YEARS = 20
        const val MAX_YEARS = 120
    }
}
