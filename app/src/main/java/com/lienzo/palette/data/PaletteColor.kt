package com.lienzo.palette.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PaletteColor(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hex: String,          // #RRGGBB
    val createdAt: Long = System.currentTimeMillis()
) {
    val spec: ColorMath.Spec
        get() = ColorMath.specOf(ColorMath.parseHex(hex) ?: ColorMath.Rgb(0, 0, 0))
}

/** 各色彩定义的纯文本构建（复制用） */
object FormatText {
    fun hex(c: PaletteColor) = c.spec.hex
    fun rgb(c: PaletteColor) = with(c.spec.rgb) { "RGB($r, $g, $b)" }
    fun hsv(c: PaletteColor) = with(c.spec.hsv) { "HSV(${i(h)}, ${i(s)}%, ${i(v)}%)" }
    fun hsl(c: PaletteColor) = with(c.spec.hsl) { "HSL(${i(h)}, ${i(s)}%, ${i(l)}%)" }
    fun cmyk(col: PaletteColor) = col.spec.cmyk.let { "CMYK(${i(it.c)}%, ${i(it.m)}%, ${i(it.y)}%, ${i(it.k)}%)" }
    fun hsb(col: PaletteColor) = col.spec.hsv.let { "HSB(${i(it.h)}, ${i(it.s)}%, ${i(it.v)}%)" }
    fun ncs(col: PaletteColor) = col.spec.ncs.notation()
    fun lab(col: PaletteColor) = col.spec.lab.let { "LAB(${d(it.l)}, ${d(it.a)}, ${d(it.b)})" }
    fun xyz(col: PaletteColor) = col.spec.xyz.let { "XYZ(${d(it.x)}, ${d(it.y)}, ${d(it.z)})" }
    fun lch(col: PaletteColor) = col.spec.lch.let { "LCh(${d(it.l)}, ${d(it.c)}, ${d(it.h)})" }
    fun xy(col: PaletteColor) = col.spec.xy.let { "CIE-xy(${d(it.x, 4)}, ${d(it.y, 4)})" }

    fun all(c: PaletteColor): String = listOf(
        "${c.name} ${hex(c)}", rgb(c), hsb(c), hsv(c), hsl(c), cmyk(c), ncs(c),
        lab(c), lch(c), xy(c)
    ).joinToString("\n")

    private fun i(v: Double) = v.toInt().toString()
    private fun d(v: Double, p: Int = 1) = String.format("%.${p}f", v)
}

/** 各色彩定义标准的简要描述（UI 中以小字展示） */
object FormatDescriptions {
    data class Info(val title: String, val desc: String)

    val entries: List<Pair<String, (PaletteColor) -> String>> = listOf(
        "HEX" to FormatText::hex,
        "RGB" to FormatText::rgb,
        "HSB" to FormatText::hsb,
        "HSV" to FormatText::hsv,
        "HSL" to FormatText::hsl,
        "CMYK" to FormatText::cmyk,
        "NCS" to FormatText::ncs,
        "CIELAB" to FormatText::lab,
        "LCh(ab)" to FormatText::lch,
        "CIE-xy" to FormatText::xy,
    )

    fun of(key: String): String = when (key) {
        "HEX" -> "十六进制网页色值（sRGB），以 #RRGGBB 表示，Web/CSS 通用。"
        "RGB" -> "红绿蓝三通道加色模型（0–255），显示器、屏幕发光设备的原生色彩空间 sRGB。"
        "HSB" -> "色相/饱和度/亮度（Brightness），即 HSV 的别名，Adobe 系软件常用称呼。"
        "HSV" -> "色相 H(0–360°)、饱和度 S、明度 V(0–100%)，面向用户的直觉圆柱坐标模型。"
        "HSL" -> "色相/饱和度/ Lightness 亮度，中点为 50% 灰，CSS 与设计工具常用。"
        "CMYK" -> "青/品红/黄/黑四色减色印刷模型（0–100%），用于印刷四色套印。"
        "NCS" -> "瑞典自然色彩系统，以黑度、彩度与基本色相描述（如 S 2080-Y20R）；本应用为几何近似换算，非官方精确值。"
        "CIELAB" -> "CIE 1976 L*a*b* 均匀感知色彩空间：L* 明度，a* 红绿轴，b* 黄蓝轴，色差计算标准。"
        "LCh(ab)" -> "LAB 的极坐标形式：L* 明度、C 彩度、h 色相角，便于调整鲜艳度与色相。"
        "CIE-xy" -> "CIE 1931 色度图坐标 (x, y)，用于在色域马蹄图中标注颜色的物理色度位置。"
        else -> ""
    }
}
