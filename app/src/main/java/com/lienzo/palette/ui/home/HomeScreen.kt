package com.lienzo.palette.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lienzo.palette.data.ColorRepository
import com.lienzo.palette.data.FormatText
import com.lienzo.palette.data.PaletteColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onAdd: () -> Unit, onOpenColor: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val colors by ColorRepository.colors.collectAsState()

    var menuOpen by remember { mutableStateOf(false) }
    var urlDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PaletteColor?>(null) }
    var renameText by remember { mutableStateOf("") }

    // 视图设置（记忆到 SharedPreferences，重启保留）
    val prefs = remember { ctx.getSharedPreferences("lienzo_ui", 0) }
    var tileView by rememberSaveable { mutableStateOf(prefs.getBoolean("tileView", false)) }
    var columns by rememberSaveable { mutableIntStateOf(prefs.getInt("columns", 2)) }
    var rows by rememberSaveable { mutableIntStateOf(prefs.getInt("rows", 2)) }
    var aboutDialog by remember { mutableStateOf(false) }
    var animDialog by remember { mutableStateOf(false) }
    var animSpeed by rememberSaveable { mutableFloatStateOf(prefs.getFloat("animSpeed", 1f)) }

    fun persistView() {
        prefs.edit().putBoolean("tileView", tileView).putInt("columns", columns).putInt("rows", rows).apply()
    }

    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { ColorRepository.importFromFile(ctx, uri) }
                .onSuccess { Toast.makeText(ctx, "已导入 $it 个颜色", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(ctx, "导入失败：${it.message}", Toast.LENGTH_SHORT).show() }
        }
    }
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { ColorRepository.exportTo(ctx, uri) }
                .onSuccess { Toast.makeText(ctx, "已导出", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(ctx, "导出失败：${it.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lienzo", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { tileView = !tileView; persistView() }) {
                        Icon(
                            if (tileView) Icons.Default.ViewList else Icons.Default.GridView,
                            if (tileView) "切换为列表视图" else "切换为磁贴视图"
                        )
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("从文件导入 JSON") },
                            leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                            onClick = { menuOpen = false; importFile.launch(arrayOf("application/json", "text/*")) }
                        )
                        DropdownMenuItem(
                            text = { Text("从网页链接导入") },
                            leadingIcon = { Icon(Icons.Default.Public, null) },
                            onClick = { menuOpen = false; urlDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("导出为 JSON") },
                            leadingIcon = { Icon(Icons.Default.FileDownload, null) },
                            onClick = { menuOpen = false; exportFile.launch("lienzo-palette.json") }
                        )
                        DropdownMenuItem(
                            text = { Text("动画速率") },
                            leadingIcon = { Icon(Icons.Default.Speed, null) },
                            onClick = { menuOpen = false; animDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("关于 Lienzo") },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                            onClick = { menuOpen = false; aboutDialog = true }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "添加颜色") }
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            if (colors.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无颜色，点击 + 添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (!tileView) {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(colors, key = { it.id }) { c ->
                        ColorCard(
                            color = c,
                            onClick = { onOpenColor(c.id) },
                            onLongClick = {
                                clipboard.setText(AnnotatedString(FormatText.all(c)))
                                Toast.makeText(ctx, "已复制 ${c.name} 的全部色彩定义", Toast.LENGTH_SHORT).show()
                            },
                            onRename = { renameTarget = c; renameText = c.name },
                            onDelete = { scope.launch { ColorRepository.delete(ctx, c.id) } }
                        )
                    }
                }
            } else {
                Column {
                    // 行 / 列滑条
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("列", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = columns.toFloat(),
                            onValueChange = { columns = it.toInt(); persistView() },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.weight(1f)
                        )
                        Text("$columns", fontSize = 12.sp, modifier = Modifier.width(18.dp))
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("行", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = rows.toFloat(),
                            onValueChange = { rows = it.toInt(); persistView() },
                            valueRange = 1f..4f,
                            steps = 2,
                            modifier = Modifier.weight(1f)
                        )
                        Text("$rows", fontSize = 12.sp, modifier = Modifier.width(18.dp))
                    }
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(columns),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                        verticalItemSpacing = 10.dp,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(colors, key = { it.id }) { c ->
                            ColorTile(
                                color = c,
                                rowUnits = rows,
                                onClick = { onOpenColor(c.id) },
                                onLongClick = {
                                    clipboard.setText(AnnotatedString(FormatText.all(c)))
                                    Toast.makeText(ctx, "已复制 ${c.name} 的全部色彩定义", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (urlDialog) {
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { urlDialog = false },
            title = { Text("从网页链接导入") },
            text = {
                Column {
                    Text(
                        "输入指向 JSON 颜色文件的 URL",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = url, onValueChange = { url = it }, singleLine = true,
                        placeholder = { Text("https://example.com/colors.json") })
                }
            },
            confirmButton = {
                TextButton(
                    enabled = url.startsWith("http"),
                    onClick = {
                        urlDialog = false
                        scope.launch {
                            runCatching { ColorRepository.importFromUrl(ctx, url.trim()) }
                                .onSuccess { Toast.makeText(ctx, "已导入 $it 个颜色", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(ctx, "导入失败：${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    }
                ) { Text("导入") }
            },
            dismissButton = { TextButton(onClick = { urlDialog = false }) { Text("取消") } }
        )
    }

    if (animDialog) {
        AlertDialog(
            onDismissRequest = { animDialog = false },
            title = { Text("动画速率") },
            text = {
                Column {
                    Text(
                        "%.1fx（0.5x 慢速 ~ 2.0x 快速）".format(animSpeed),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = animSpeed,
                        onValueChange = {
                            animSpeed = it
                            com.lienzo.palette.UiSettings.animSpeed = it
                            prefs.edit().putFloat("animSpeed", it).apply()
                        },
                        valueRange = 0.5f..2f
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            animSpeed = 1f
                            com.lienzo.palette.UiSettings.animSpeed = 1f
                            prefs.edit().putFloat("animSpeed", 1f).apply()
                        }) { Text("重置 1.0x") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { animDialog = false }) { Text("完成") } }
        )
    }

    if (aboutDialog) {
        AlertDialog(
            onDismissRequest = { aboutDialog = false },
            title = { Text("Lienzo") },
            text = {
                Column {
                    Text("调色盘 · 版本 build2", fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("发布者 / 开发者：Yipinware", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("Kotlin + Jetpack Compose · Material 3", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { aboutDialog = false }) { Text("关闭") } }
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名颜色") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { ColorRepository.rename(ctx, target.id, renameText.trim().ifBlank { target.hex }) }
                    renameTarget = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorCard(
    color: PaletteColor,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(parse(color.hex), CircleShape)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(color.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(color.hex, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "长按复制全部定义 · 点击查看详情",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onRename) { Icon(Icons.Default.Edit, "重命名", Modifier.size(18.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除", Modifier.size(18.dp)) }
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(color.hex))
                Toast.makeText(ctx, "已复制 ${color.hex}", Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Default.ContentCopy, "复制 HEX", Modifier.size(18.dp)) }
        }
    }
}

internal fun parse(hex: String): Color =
    Color(android.graphics.Color.parseColor(hex))

/** 交错磁贴：高度随“行”滑条与颜色属性变化，形成瀑布效果 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorTile(
    color: PaletteColor,
    rowUnits: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val spec = color.spec
    // 瀑布错落：以 HSV 亮度/饱和度派生高度差，颜色不同 → 磁贴高低不同
    val stagger = ((spec.hsv.v * 0.24 + spec.hsl.s * 0.12) / 100 * 3).toInt() // 0..3
    val swatchH = (72 + rowUnits * 28 + stagger * 14).dp
    val onTile = if (spec.hsv.v < 55.0) Color.White else Color(0xFF1A1A1A)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(swatchH)
                    .background(parse(color.hex)),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    color.hex,
                    color = onTile.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    color.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "RGB ${spec.rgb.r}, ${spec.rgb.g}, ${spec.rgb.b}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
