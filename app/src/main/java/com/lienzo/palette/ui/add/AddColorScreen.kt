package com.lienzo.palette.ui.add

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lienzo.palette.data.ColorMath
import com.lienzo.palette.data.ColorRepository
import com.lienzo.palette.data.FormatDescriptions
import kotlinx.coroutines.launch

private enum class Mode(val label: String, val hint: String) {
    HEX("HEX", "#RRGGBB 或 #FFF"),
    RGB("RGB", "如 22, 105, 208 或 #1669D0"),
    HSL("HSL", "如 210, 60%, 45%"),
    HSV("HSV/HSB", "如 210, 72%, 82%"),
    CMYK("CMYK", "如 85, 50, 0, 18"),
    NCS("NCS", "如 S 2080-Y20R"),
    LAB("LAB", "如 45.2, 12.5, -40.8"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddColorScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(Mode.HEX) }
    var input by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    val parsed: ColorMath.Rgb? = parseInput(mode, input)
    val spec = parsed?.let { ColorMath.specOf(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加颜色") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Mode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m; input = "" },
                        shape = SegmentedButtonDefaults.itemShape(i, Mode.entries.size)
                    ) { Text(m.label, fontSize = 12.sp) }
                }
            }

            Text(
                mode.hint,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(mode.label) },
                singleLine = true,
                supportingText = {
                    Text(FormatDescriptions.of(mode.name), fontSize = 11.sp)
                },
                isError = input.isNotBlank() && parsed == null,
                modifier = Modifier.fillMaxWidth()
            )

            // 实时预览
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .background(
                            spec?.let { parseHex(it.hex) } ?: MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(spec?.hex ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    spec?.let {
                        Text(
                            com.lienzo.palette.data.FormatText.rgb(
                                PaletteColorOf(spec)
                            ),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("颜色名称（可选，默认使用色值）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = parsed != null,
                onClick = {
                    scope.launch {
                        ColorRepository.add(ctx, name.trim().ifBlank { spec!!.hex }, spec!!.hex)
                        Toast.makeText(ctx, "已添加 ${spec!!.hex}", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存颜色") }
        }
    }
}

private fun parseHex(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

/** 便捷：从 spec 构造一个临时 PaletteColor 以复用 FormatText */
private fun PaletteColorOf(spec: ColorMath.Spec): com.lienzo.palette.data.PaletteColor =
    com.lienzo.palette.data.PaletteColor(name = "", hex = spec.hex)

private fun parseInput(mode: Mode, input: String): ColorMath.Rgb? {
    val s = input.trim()
    if (s.isBlank()) return null
    return when (mode) {
        Mode.HEX -> ColorMath.parseHex(s)
        Mode.RGB -> {
            val parts = s.replace("#", "").split(Regex("[,\\s]+")).filter { it.isNotBlank() }
            if (parts.size == 3) parts.mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 }
                ?.let { ColorMath.Rgb(it[0], it[1], it[2]) }?.clamp()
            else ColorMath.parseHex(s) // 也允许直接输入 hex
        }
        Mode.HSL -> {
            val n = extractNumbers(s)
            if (n.size >= 3) ColorMath.hslToRgb(ColorMath.Hsl(n[0], n[1], n[2])) else null
        }
        Mode.HSV -> {
            val n = extractNumbers(s)
            if (n.size >= 3) ColorMath.hsvToRgb(ColorMath.Hsv(n[0], n[1], n[2])) else null
        }
        Mode.CMYK -> {
            val n = extractNumbers(s)
            if (n.size >= 4) ColorMath.cmykToRgb(ColorMath.Cmyk(n[0], n[1], n[2], n[3])) else null
        }
        Mode.NCS -> ColorMath.parseNcs(s)
        Mode.LAB -> {
            val n = extractNumbers(s)
            if (n.size >= 3) ColorMath.xyzToRgb(ColorMath.labToXyz(ColorMath.Lab(n[0], n[1], n[2]))) else null
        }
    }
}

private fun extractNumbers(s: String): List<Double> =
    Regex("-?\\d+(?:\\.\\d+)?").findAll(s).map { it.value.toDouble() }.toList()
