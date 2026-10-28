package com.lienzo.palette.ui.detail

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lienzo.palette.data.ColorMath
import com.lienzo.palette.data.ColorRepository
import com.lienzo.palette.data.FormatDescriptions
import com.lienzo.palette.data.FormatText
import com.lienzo.palette.data.PaletteColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(colorId: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val colors by ColorRepository.colors.collectAsState()
    val color = colors.firstOrNull { it.id == colorId }

    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    if (color == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("颜色不存在") }
        return
    }
    val spec = color.spec

    fun copy(label: String, text: String) {
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(ctx, "已复制 $label", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(color.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    IconButton(onClick = { renameText = color.name; renameOpen = true }) {
                        Icon(Icons.Default.Edit, "重命名")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            ColorRepository.delete(ctx, color.id)
                            onBack()
                        }
                    }) { Icon(Icons.Default.Delete, "删除") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 大色块
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(parseC(color.hex), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        color.name,
                        color = onColorFor(spec),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        spec.hex,
                        color = onColorFor(spec).copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            // 各色彩定义 + 复制按钮 + 标准简述
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                FormatDescriptions.entries.forEachIndexed { i, (key, fn) ->
                    val value = fn(color)
                    Column(Modifier.fillMaxWidth()) {
                        if (i > 0) HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(key, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(2.dp))
                                Text(value, fontSize = 15.sp)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    FormatDescriptions.of(key),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { copy(key, value) }) {
                                Icon(Icons.Default.ContentCopy, "复制 $key", Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 色域图（CIE 1931 马蹄图 + sRGB 三角 + 位置标注）
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("色域位置 · CIE 1931 色度图", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    GamutPlot(spec)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${FormatText.xy(color)} · 位于 sRGB 色域三角形${if (inSrgb(spec)) "内" else "外（超出 sRGB，显示为最接近的可显示色）"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 明度 / 纯度 / 亮度等指标
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("感知属性", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    MetricBar("明度 L*", (spec.lab.l / 100.0).toFloat(), "%.1f".format(spec.lab.l))
                    MetricBar("纯度 / 彩度 C*", (spec.lch.c / 130.0).coerceIn(0.0, 1.0).toFloat(), "%.1f".format(spec.lch.c))
                    MetricBar("亮度 V (HSB)", (spec.hsv.v / 100.0).toFloat(), "%.0f%%".format(spec.hsv.v))
                    MetricBar("饱和度 S (HSL)", (spec.hsl.s / 100.0).toFloat(), "%.0f%%".format(spec.hsl.s))
                    MetricBar(
                        "相对亮度 (WCAG)",
                        relativeLum(spec),
                        "%.3f · 与白色对比度 %.2f:1".format(relativeLum(spec), ColorMath.contrast(spec.rgb, ColorMath.Rgb(255, 255, 255)))
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text("重命名颜色") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        ColorRepository.rename(ctx, color.id, renameText.trim().ifBlank { color.hex })
                    }
                    renameOpen = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameOpen = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun MetricBar(label: String, frac: Float, value: String, color: Color = MaterialTheme.colorScheme.primary) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp)
            Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

/** CIE 1931 马蹄图：光谱轨迹 + sRGB 三角 + 当前颜色标记 */
@Composable
private fun GamutPlot(spec: ColorMath.Spec) {
    val outline = MaterialTheme.colorScheme.outline
    val marker = parseC(spec.hex)
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
    ) {
        val pad = 12.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        // xy 显示范围：x 0..0.8, y 0..0.9
        fun px(p: ColorMath.Xy) = Offset(pad + (p.x / 0.8).toFloat() * w, pad + (1 - p.y / 0.9).toFloat() * h)

        // 光谱轨迹填充
        val path = Path()
        ColorMath.spectralLocus.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(px(p).x, px(p).y) else path.lineTo(px(p).x, px(p).y)
        }
        // 紫线闭合（380nm -> 700nm 直线）
        path.lineTo(px(ColorMath.spectralLocus.first()).x, px(ColorMath.spectralLocus.first()).y)
        drawPath(path, marker.copy(alpha = 0.12f))
        drawPath(path, outline.copy(alpha = 0.6f), style = Stroke(1.5.dp.toPx()))

        // sRGB 三角
        val tri = Path().apply {
            ColorMath.srgbTriangle.forEachIndexed { i, p ->
                val o = px(p)
                if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
            }
            close()
        }
        drawPath(tri, outline, style = Stroke(1.dp.toPx()))

        // D65 白点
        val w65 = px(ColorMath.Xy(0.3127, 0.3290))
        drawCircle(outline.copy(alpha = 0.5f), 2.dp.toPx(), w65)

        // 当前颜色位置
        val pt = px(spec.xy)
        drawCircle(marker, 6.dp.toPx(), pt)
        drawCircle(Color.White, 2.5.dp.toPx(), pt)
    }
}

private fun inSrgb(spec: ColorMath.Spec): Boolean {
    // 点在三角形内：重心同侧判定
    val (a, b, c) = ColorMath.srgbTriangle
    val p = spec.xy
    fun sign(p1: ColorMath.Xy, p2: ColorMath.Xy, p3: ColorMath.Xy) =
        (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
    val d1 = sign(p, a, b); val d2 = sign(p, b, c); val d3 = sign(p, c, a)
    val neg = (d1 < 0) || (d2 < 0) || (d3 < 0)
    val pos = (d1 > 0) || (d2 > 0) || (d3 > 0)
    return !(neg && pos)
}

private fun relativeLum(spec: ColorMath.Spec): Float =
    ColorMath.relativeLuminance(spec.rgb).toFloat()

private fun onColorFor(spec: ColorMath.Spec): Color =
    if (spec.hsv.v < 55.0) Color.White else Color(0xFF1A1A1A)

private fun parseC(hex: String): Color = Color(android.graphics.Color.parseColor(hex))
