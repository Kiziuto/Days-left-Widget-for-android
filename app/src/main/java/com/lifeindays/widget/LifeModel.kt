package com.lifeindays.widget

import android.content.Context
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Days between [dob] and [dob] + [years], split into lived and remaining as of [today]. */
class LifeModel(
    val dob: LocalDate,
    val years: Int,
    val today: LocalDate = LocalDate.now()
) {
    val total: Int = ChronoUnit.DAYS.between(dob, dob.plusYears(years.toLong())).toInt()
    val elapsed: Int = ChronoUnit.DAYS.between(dob, today).toInt().coerceIn(0, total)
    val remaining: Int get() = total - elapsed
    val percent: Double get() = elapsed * 100.0 / total
}

object Fmt {
    fun int(n: Int): String = NumberFormat.getIntegerInstance().format(n)
    fun percent(p: Double): String = String.format(Locale.getDefault(), "%.1f%%", p)
}

/** Settings shared by the widget and its settings screen. */
object LifeStore {
    val DEFAULT_DOB: LocalDate = LocalDate.of(2000, 11, 22)
    const val DEFAULT_YEARS = 90

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences("life_in_days", Context.MODE_PRIVATE)

    fun isConfigured(c: Context) = prefs(c).contains("dob")

    fun dob(c: Context): LocalDate =
        LocalDate.ofEpochDay(prefs(c).getLong("dob", DEFAULT_DOB.toEpochDay()))

    fun years(c: Context): Int = prefs(c).getInt("years", DEFAULT_YEARS)

    fun save(c: Context, dob: LocalDate, years: Int) {
        prefs(c).edit().putLong("dob", dob.toEpochDay()).putInt("years", years).apply()
    }

    /** Null until the user has saved settings once. */
    fun model(c: Context): LifeModel? =
        if (isConfigured(c)) LifeModel(dob(c), years(c)) else null
}
