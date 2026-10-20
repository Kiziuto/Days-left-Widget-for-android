package com.lifeindays.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.TextView
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object Neon {
    val CYAN = 0xFF00E5FF.toInt()
    val VIOLET = 0xFF8A5CFF.toInt()
    val ICE = 0xFFEAFBFF.toInt()
    val BG_TOP = 0xFF0C1626.toInt()
    val BG_BOTTOM = 0xFF04060B.toInt()
    val PAGE = 0xFF05070D.toInt()

    fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = ((a shr shift and 0xFF) + ((b shr shift and 0xFF) - (a shr shift and 0xFF)) * t).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
}

/** Orbitron (SIL OFL) from assets, with a system fallback so a font problem can never break the widget. */
object Fonts {
    private var face: Typeface? = null
    private var tried = false

    private fun orbitron(context: Context): Typeface? {
        if (!tried) {
            tried = true
            face = try {
                Typeface.createFromAsset(context.applicationContext.assets, "orbitron.ttf")
            } catch (e: Exception) {
                null
            }
        }
        return face
    }

    fun apply(paint: Paint, context: Context, weight: Int) {
        val tf = orbitron(context)
        if (tf != null) {
            paint.typeface = tf
            paint.fontVariationSettings = "'wght' $weight"
        } else {
            paint.typeface = Typeface.create("sans-serif-condensed", if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    fun apply(view: TextView, weight: Int) {
        val tf = orbitron(view.context)
        if (tf != null) {
            view.typeface = tf
            view.fontVariationSettings = "'wght' $weight"
        } else {
            view.typeface = Typeface.create("sans-serif-condensed", if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}

/**
 * Draws the whole widget into one bitmap: dark HUD panel, corner brackets, glowing days-left
 * number, and a segmented progress bar. A bitmap gives pixel-exact control (glow, gradients)
 * that RemoteViews layouts can't do.
 */
object WidgetRenderer {

    fun describe(m: LifeModel?): String =
        if (m == null) "Life in Days widget. Tap to set your date of birth."
        else "${Fmt.int(m.remaining)} days remaining out of a ${m.years} year life"

    fun render(context: Context, m: LifeModel?, widthPx: Int, heightPx: Int): Bitmap {
        val res = context.resources
        val d = res.displayMetrics.density
        val sp = d * res.configuration.fontScale
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()

        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = 22f * d

        // ---- Panel: clipped to a rounded rect so nothing pokes past the corners.
        c.save()
        c.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, w, h), radius, radius, Path.Direction.CW) })
        p.shader = LinearGradient(0f, 0f, 0f, h, Neon.BG_TOP, Neon.BG_BOTTOM, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(
            w / 2f, h * 0.48f, max(w, h) * 0.6f,
            Neon.withAlpha(Neon.CYAN, 0x2A), Neon.withAlpha(Neon.CYAN, 0x00), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        p.color = Neon.withAlpha(0xFFFFFFFF.toInt(), 0x0B)       // faint scanlines
        var y = 2f * d
        while (y < h) {
            c.drawRect(0f, y, w, y + max(1f, 0.6f * d), p)
            y += 4f * d
        }
        c.restore()

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f * d
        p.color = Neon.withAlpha(Neon.CYAN, 0x55)
        val half = p.strokeWidth / 2
        c.drawRoundRect(half, half, w - half, h - half, radius, radius, p)

        // ---- HUD corner brackets.
        val inset = 9f * d
        val len = 12f * d
        p.strokeWidth = 1.6f * d
        p.strokeCap = Paint.Cap.ROUND
        p.color = Neon.withAlpha(Neon.CYAN, 0xCC)
        val br = Path()
        br.moveTo(inset, inset + len); br.lineTo(inset, inset); br.lineTo(inset + len, inset)
        br.moveTo(w - inset - len, inset); br.lineTo(w - inset, inset); br.lineTo(w - inset, inset + len)
        br.moveTo(inset, h - inset - len); br.lineTo(inset, h - inset); br.lineTo(inset + len, h - inset)
        br.moveTo(w - inset - len, h - inset); br.lineTo(w - inset, h - inset); br.lineTo(w - inset, h - inset - len)
        c.drawPath(br, p)
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.BUTT

        val pad = 20f * d
        val availW = w - 2 * pad
        val ts = min(max(h * 0.075f, 9f * sp), 12f * sp)

        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Neon.withAlpha(Neon.CYAN, 0xD0)
            textSize = ts
            letterSpacing = 0.16f
        }
        Fonts.apply(label, context, 600)

        // ---- Top label with status LED.
        val labelBase = pad * 0.9f + ts
        p.color = Neon.CYAN
        c.drawCircle(pad + 2.5f * d, labelBase - ts * 0.36f, 2.5f * d, p)
        c.drawText(if (m == null) "LIFE COUNTDOWN" else "DAYS REMAINING", pad + 11f * d, labelBase, label)

        val num = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; letterSpacing = 0.04f }
        Fonts.apply(num, context, 800)

        if (m == null) {
            drawFitted(c, num, "TAP TO SET UP", w / 2f, h * 0.55f, availW * 0.9f, h * 0.22f, 0.5f)
            return bmp
        }

        // ---- Bottom stats and segmented bar.
        val stats = Paint(label).apply { color = Neon.withAlpha(Neon.ICE, 0x99); letterSpacing = 0.1f }
        Fonts.apply(stats, context, 500)
        val statsBase = h - 16f * d
        c.drawText("${Fmt.percent(m.percent)} ELAPSED", pad, statsBase, stats)
        stats.textAlign = Paint.Align.RIGHT
        c.drawText("${Fmt.int(m.elapsed)} LIVED", w - pad, statsBase, stats)

        val barH = 5f * d
        val barBottom = statsBase - ts - 8f * d
        val barTop = barBottom - barH
        var segs = 36
        val gap = 2f * d
        while (segs > 8 && (availW - gap * (segs - 1)) / segs < 2f * d) segs--
        val segW = (availW - gap * (segs - 1)) / segs
        val lit = (m.percent / 100.0 * segs).toFloat()
        val full = floor(lit).toInt()
        val frac = lit - full
        for (i in 0 until segs) {
            val x = pad + i * (segW + gap)
            val base = Neon.mix(Neon.CYAN, Neon.VIOLET, i / (segs - 1f).coerceAtLeast(1f))
            p.color = when {
                i < full -> base
                i == full && frac > 0.05f -> Neon.withAlpha(base, (frac * 255).toInt())
                else -> Neon.withAlpha(0xFFFFFFFF.toInt(), 0x1A)
            }
            c.drawRoundRect(x, barTop, x + segW, barBottom, d, d, p)
        }

        // ---- The number, with a cyan glow underneath a crisp ice-white copy.
        drawFitted(
            c, num, Fmt.int(m.remaining), w / 2f, (labelBase + 10f * d + barTop - 10f * d) / 2f,
            availW * 0.94f, barTop - 10f * d - (labelBase + 10f * d), 0.9f
        )
        return bmp
    }

    /** Draws [text] centred on ([cx],[cy]), shrinking until it fits maxW x maxH, with neon glow. */
    private fun drawFitted(
        c: Canvas, paint: Paint, text: String, cx: Float, cy: Float, maxW: Float, maxH: Float, heightUse: Float
    ) {
        val b = Rect()
        var size = max(maxH / 0.72f, 8f)
        for (i in 0 until 40) {
            paint.textSize = size
            paint.getTextBounds(text, 0, text.length, b)
            if ((b.width() <= maxW && b.height() <= maxH * heightUse) || size <= 8f) break
            size *= 0.94f
        }
        val baseline = cy - (b.top + b.bottom) / 2f
        val glow = Paint(paint).apply {
            color = Neon.withAlpha(Neon.CYAN, 0xD0)
            maskFilter = BlurMaskFilter(max(size * 0.12f, 2f), BlurMaskFilter.Blur.NORMAL)
        }
        c.drawText(text, cx, baseline, glow)
        paint.color = Neon.ICE
        c.drawText(text, cx, baseline, paint)
    }
}
