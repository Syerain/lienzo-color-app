package com.lienzo.palette.data

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 纯 Kotlin 色彩数学：HEX / RGB / CMYK / HSV(HSB) / HSL / XYZ / LAB / LCh / CIE-xy */
object ColorMath {

    data class Rgb(val r: Int, val g: Int, val b: Int) {
        fun clamp() = Rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
        fun toHex(): String = "#" + listOf(r, g, b).joinToString("") {
            it.coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()
        }
    }
    data class Hsv(val h: Double, val s: Double, val v: Double) // h 0-360, s/v 0-100
    data class Hsl(val h: Double, val s: Double, val l: Double)
    data class Cmyk(val c: Double, val m: Double, val y: Double, val k: Double) // 0-100
    data class Lab(val l: Double, val a: Double, val b: Double)
    data class Xyz(val x: Double, val y: Double, val z: Double)
    data class Lch(val l: Double, val c: Double, val h: Double)
    data class Xy(val x: Double, val y: Double)
    /** NCS 自然色彩系统（近似换算）：s=黑度, c=彩度, hue 形如 "Y20R"/"N" */
    data class Ncs(val s: Int, val c: Int, val hue: String) {
        fun notation() = "NCS S %02d%02d-%s".format(s, c, hue)
    }

    data class Spec(
        val hex: String, val rgb: Rgb, val hsv: Hsv, val hsl: Hsl, val cmyk: Cmyk,
        val lab: Lab, val xyz: Xyz, val lch: Lch, val xy: Xy, val ncs: Ncs
    )

    // ---------- HEX ----------
    fun parseHex(input: String): Rgb? {
        val s = input.trim().removePrefix("#")
        val hex = when (s.length) {
            3 -> s.map { "$it$it" }.joinToString("")
            6 -> s
            8 -> s.drop(2) // AARRGGBB / RRGGBBAA -> 取 RGB
            else -> return null
        }
        if (hex.any { it.lowercaseChar() !in "0123456789abcdef" }) return null
        return Rgb(hex.substring(0, 2).toInt(16), hex.substring(2, 4).toInt(16), hex.substring(4, 6).toInt(16))
    }

    // ---------- RGB <-> HSV ----------
    fun rgbToHsv(rgb: Rgb): Hsv {
        val r = rgb.r / 255.0; val g = rgb.g / 255.0; val b = rgb.b / 255.0
        val maxC = max(r, max(g, b)); val minC = min(r, min(g, b)); val d = maxC - minC
        val h = when {
            d == 0.0 -> 0.0
            maxC == r -> 60.0 * (((g - b) / d) % 6.0)
            maxC == g -> 60.0 * ((b - r) / d + 2.0)
            else -> 60.0 * ((r - g) / d + 4.0)
        }.let { if (it < 0) it + 360 else it }
        val s = if (maxC == 0.0) 0.0 else d / maxC
        return Hsv(h, s * 100, maxC * 100)
    }

    fun hsvToRgb(hsv: Hsv): Rgb {
        val h = ((hsv.h % 360) + 360) % 360; val s = hsv.s / 100.0; val v = hsv.v / 100.0
        val c = v * s; val x = c * (1 - abs((h / 60.0) % 2 - 1)); val m = v - c
        val (r1, g1, b1) = when (floor(h / 60.0).toInt() % 6) {
            0 -> Triple(c, x, 0.0); 1 -> Triple(x, c, 0.0); 2 -> Triple(0.0, c, x)
            3 -> Triple(0.0, x, c); 4 -> Triple(x, 0.0, c); else -> Triple(c, 0.0, x)
        }
        return Rgb(((r1 + m) * 255).roundToInt(), ((g1 + m) * 255).roundToInt(), ((b1 + m) * 255).roundToInt())
    }

    // ---------- RGB <-> HSL ----------
    fun rgbToHsl(rgb: Rgb): Hsl {
        val r = rgb.r / 255.0; val g = rgb.g / 255.0; val b = rgb.b / 255.0
        val maxC = max(r, max(g, b)); val minC = min(r, min(g, b)); val d = maxC - minC
        val l = (maxC + minC) / 2
        val h = when {
            d == 0.0 -> 0.0
            maxC == r -> 60.0 * (((g - b) / d) % 6.0)
            maxC == g -> 60.0 * ((b - r) / d + 2.0)
            else -> 60.0 * ((r - g) / d + 4.0)
        }.let { if (it < 0) it + 360 else it }
        val s = if (d == 0.0) 0.0 else d / (1 - abs(2 * l - 1))
        return Hsl(h, s * 100, l * 100)
    }

    fun hslToRgb(hsl: Hsl): Rgb {
        val h = ((hsl.h % 360) + 360) % 360; val s = (hsl.s / 100.0).coerceIn(0.0, 1.0); val l = (hsl.l / 100.0).coerceIn(0.0, 1.0)
        val c = (1 - abs(2 * l - 1)) * s; val x = c * (1 - abs((h / 60.0) % 2 - 1)); val m = l - c / 2
        val (r1, g1, b1) = when (floor(h / 60.0).toInt() % 6) {
            0 -> Triple(c, x, 0.0); 1 -> Triple(x, c, 0.0); 2 -> Triple(0.0, c, x)
            3 -> Triple(0.0, x, c); 4 -> Triple(x, 0.0, c); else -> Triple(c, 0.0, x)
        }
        return Rgb(((r1 + m) * 255).roundToInt(), ((g1 + m) * 255).roundToInt(), ((b1 + m) * 255).roundToInt())
    }

    // ---------- RGB <-> CMYK ----------
    fun rgbToCmyk(rgb: Rgb): Cmyk {
        val r = rgb.r / 255.0; val g = rgb.g / 255.0; val b = rgb.b / 255.0
        val k = 1 - max(r, max(g, b))
        if (k >= 1.0 - 1e-9) return Cmyk(0.0, 0.0, 0.0, 100.0)
        return Cmyk((1 - r - k) / (1 - k) * 100, (1 - g - k) / (1 - k) * 100, (1 - b - k) / (1 - k) * 100, k * 100)
    }

    fun cmykToRgb(cmyk: Cmyk): Rgb {
        val c = cmyk.c / 100.0; val m = cmyk.m / 100.0; val y = cmyk.y / 100.0; val k = cmyk.k / 100.0
        return Rgb(
            (255 * (1 - c) * (1 - k)).roundToInt(),
            (255 * (1 - m) * (1 - k)).roundToInt(),
            (255 * (1 - y) * (1 - k)).roundToInt()
        )
    }

    // ---------- RGB <-> XYZ <-> LAB <-> LCh (sRGB, D65) ----------
    fun rgbToXyz(rgb: Rgb): Xyz {
        fun lin(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        val r = lin(rgb.r / 255.0); val g = lin(rgb.g / 255.0); val b = lin(rgb.b / 255.0)
        return Xyz(
            (r * 0.4124564 + g * 0.3575761 + b * 0.1804375) * 100,
            (r * 0.2126729 + g * 0.7151522 + b * 0.0721750) * 100,
            (r * 0.0193339 + g * 0.1191920 + b * 0.9503041) * 100
        )
    }

    fun xyzToRgb(xyz: Xyz): Rgb {
        val x = xyz.x / 100; val y = xyz.y / 100; val z = xyz.z / 100
        fun lin2(c: Double) = (if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055)
            .coerceIn(0.0, 1.0)
        val r = x * 3.2404542 + y * -1.5371385 + z * -0.4985314
        val g = x * -0.9692660 + y * 1.8760108 + z * 0.0415560
        val b = x * 0.0556434 + y * -0.2040259 + z * 1.0572252
        return Rgb((lin2(r) * 255).roundToInt(), (lin2(g) * 255).roundToInt(), (lin2(b) * 255).roundToInt())
    }

    private val wx = 95.047; private val wy = 100.0; private val wz = 108.883

    fun xyzToLab(xyz: Xyz): Lab {
        fun f(t: Double) = if (t > 0.008856) t.pow(1.0 / 3) else (7.787 * t) + 16.0 / 116
        val fx = f(xyz.x / wx); val fy = f(xyz.y / wy); val fz = f(xyz.z / wz)
        return Lab(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    fun labToXyz(lab: Lab): Xyz {
        val fy = (lab.l + 16) / 116; val fx = fy + lab.a / 500; val fz = fy - lab.b / 200
        fun fin(t: Double): Double {
            val t3 = t * t * t
            return if (t3 > 0.008856) t3 else (t - 16.0 / 116) / 7.787
        }
        return Xyz(fin(fx) * wx, fin(fy) * wy, fin(fz) * wz)
    }

    fun labToLch(lab: Lab): Lch {
        val c = sqrt(lab.a * lab.a + lab.b * lab.b)
        var h = Math.toDegrees(kotlin.math.atan2(lab.b, lab.a))
        if (h < 0) h += 360
        return Lch(lab.l, c, h)
    }

    fun lchToLab(lch: Lch): Lab {
        val rad = Math.toRadians(lch.h)
        return Lab(lch.l, lch.c * kotlin.math.cos(rad), lch.c * kotlin.math.sin(rad))
    }

    fun xyOf(xyz: Xyz): Xy {
        val sum = xyz.x + xyz.y + xyz.z
        return if (sum == 0.0) Xy(0.0, 0.0) else Xy(xyz.x / sum, xyz.y / sum)
    }

    // ---------- NCS（近似换算）----------
    // 说明：NCS 官方换算依赖专有查表数据，此处采用公开的 HSV 几何近似：
    //   黑度 s ≈ (1 - V) * 100；彩度 c ≈ S(HSV) * 100（量化到 5 步长）；
    //   色相按 NCS 基本色相 R(0°) Y(60°) G(120°) B(240°) 分段插值。仅供快速参考。

    private val ncsSegments = listOf(
        Triple(0.0, 60.0, 'R' to 'Y'),   // R -> Y
        Triple(60.0, 120.0, 'Y' to 'G'), // Y -> G
        Triple(120.0, 240.0, 'G' to 'B'),// G -> B（经青）
        Triple(240.0, 360.0, 'B' to 'R') // B -> R（经品红）
    )

    fun rgbToNcs(rgb: Rgb): Ncs {
        val hsv = rgbToHsv(rgb)
        val v = hsv.v / 100.0; val sat = hsv.s / 100.0
        val sN = ((1 - v) * 100 / 5).roundToInt() * 5
        val cN = (sat * 100 / 5).roundToInt() * 5
        if (cN < 10) return Ncs(sN.coerceAtMost(90), 0, "N")
        val h = ((hsv.h % 360) + 360) % 360
        val (a, b, pair) = ncsSegments.first { h >= it.first && h < it.second }
        val f = (h - a) / (b - a) // 0=起点基本色, 1=终点基本色
        // 记法 "Y20R" = 主色 Y，含 20% 朝向 R；f 越接近终点，朝向百分比越小
        val towardPct = ((1 - f) * 100 / 5).roundToInt() * 5
        val hue = if (towardPct <= 0) "${pair.second}" else "${pair.second}$towardPct${pair.first}"
        return Ncs(sN.coerceIn(0, 90), cN.coerceIn(0, 100), hue)
    }

    private val ncsAngle = mapOf('R' to 0.0, 'Y' to 60.0, 'G' to 120.0, 'B' to 240.0)

    /** 解析 "S 2080-Y20R" / "2080-Y20R" / "NCS 4020-G30Y" 等，返回近似 RGB */
    fun parseNcs(input: String): Rgb? {
        val m = Regex("""S?\s*(\d{2})(\d{2})\s*-?\s*([NRYGB](\d{1,2})?[NRYGB]?)""", RegexOption.IGNORE_CASE)
            .find(input.trim()) ?: return null
        val (sg, cg, hueg) = m.destructured
        val sN = sg.toInt(); val cN = cg.toInt()
        val hueStr = hueg.uppercase()
        val h: Double = if (hueStr == "N") 0.0 else {
            val base = hueStr[0]; val toward = hueStr.last()
            val pct = hueStr.drop(1).dropLast(1).toIntOrNull() ?: 0
            val a = ncsAngle[base] ?: return null
            val raw = ncsAngle[toward] ?: return null
            var diff = raw - a
            if (diff > 180) diff -= 360.0
            if (diff < -180) diff += 360.0
            val h0 = a + diff * (pct / 100.0)
            if (h0 < 0) h0 + 360.0 else h0 % 360.0
        }
        val v = 1 - sN / 100.0
        val sat = if (v <= 0.05) 0.0 else (cN / 100.0) / v
        return hsvToRgb(Hsv(h, sat.coerceIn(0.0, 1.0) * 100, v * 100))
    }

    fun specOf(rgb: Rgb): Spec {
        val rc = rgb.clamp()
        val xyz = rgbToXyz(rc)
        return Spec(
            hex = rc.toHex(), rgb = rc,
            hsv = rgbToHsv(rc), hsl = rgbToHsl(rc), cmyk = rgbToCmyk(rc),
            lab = xyzToLab(xyz), xyz = xyz, lch = labToLch(xyzToLab(xyz)), xy = xyOf(xyz),
            ncs = rgbToNcs(rc)
        )
    }

    // ---------- 相对亮度 / 对比度（WCAG）----------
    fun relativeLuminance(rgb: Rgb): Double {
        fun lin(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * lin(rgb.r / 255.0) + 0.7152 * lin(rgb.g / 255.0) + 0.0722 * lin(rgb.b / 255.0)
    }

    fun contrast(a: Rgb, b: Rgb): Double {
        val l1 = relativeLuminance(a); val l2 = relativeLuminance(b)
        val hi = max(l1, l2); val lo = min(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }

    // ---------- CIE 1931 光谱轨迹采样（用于色域马蹄图）----------
    val spectralLocus: List<Xy> = listOf(
        Xy(0.1756, 0.0052), Xy(0.1750, 0.0050), Xy(0.1700, 0.0054), Xy(0.1566, 0.0068),
        Xy(0.1440, 0.0074), Xy(0.1240, 0.0082), Xy(0.1000, 0.0098), Xy(0.0780, 0.0126),
        Xy(0.0610, 0.0170), Xy(0.0450, 0.0230), Xy(0.0330, 0.0300), Xy(0.0240, 0.0390),
        Xy(0.0170, 0.0520), Xy(0.0110, 0.0730), Xy(0.0070, 0.1040), Xy(0.0040, 0.1700),
        Xy(0.0020, 0.2900), Xy(0.0000, 0.4300), Xy(0.0000, 0.5300), Xy(0.0050, 0.6400),
        Xy(0.0200, 0.7300), Xy(0.0700, 0.8300), Xy(0.1400, 0.8600), Xy(0.2200, 0.8300),
        Xy(0.3000, 0.7600), Xy(0.3800, 0.6800), Xy(0.4800, 0.5900), Xy(0.5700, 0.5200),
        Xy(0.6500, 0.4600), Xy(0.7000, 0.4200), Xy(0.7300, 0.4000), Xy(0.7500, 0.3850),
        Xy(0.7300, 0.3300), Xy(0.7100, 0.3000), Xy(0.7000, 0.2800), Xy(0.7000, 0.2650)
    )

    /** sRGB 三原色三角 */
    val srgbTriangle = listOf(Xy(0.6400, 0.3300), Xy(0.3000, 0.6000), Xy(0.1500, 0.0600))
}
