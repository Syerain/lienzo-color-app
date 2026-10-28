package com.lienzo.palette.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 颜色仓库：JSON 文件持久化（filesDir/palette.json）+ 导入 / 导出 / URL 导入。
 * 导出格式：
 * [
 *   {"name":"...,","hex":"#RRGGBB","createdAt":0,"id":"..."}
 * ]
 * 导入兼容：数组、{"colors":[...]}、宽松字段名（hex/color/value）。
 */
object ColorRepository {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val serializer = ListSerializer(PaletteColor.serializer())

    private val _colors = MutableStateFlow<List<PaletteColor>>(emptyList())
    val colors: StateFlow<List<PaletteColor>> = _colors

    private fun file(ctx: Context) = File(ctx.filesDir, "palette.json")

    fun load(ctx: Context) {
        val f = file(ctx)
        _colors.value = if (f.exists()) {
            runCatching { json.decodeFromString(serializer, f.readText()) }.getOrDefault(emptyList())
        } else emptyList()
    }

    suspend fun save(ctx: Context) = withContext(Dispatchers.IO) {
        runCatching { file(ctx).writeText(json.encodeToString(serializer, _colors.value)) }
    }

    private suspend fun commit(ctx: Context, list: List<PaletteColor>) {
        _colors.value = list
        save(ctx)
    }

    suspend fun add(ctx: Context, name: String, hex: String) {
        val c = PaletteColor(name = name.ifBlank { hex }, hex = ColorMath.parseHex(hex)!!.toHex())
        commit(ctx, _colors.value + c)
    }

    suspend fun rename(ctx: Context, id: String, name: String) =
        commit(ctx, _colors.value.map { if (it.id == id) it.copy(name = name) else it })

    suspend fun delete(ctx: Context, id: String) =
        commit(ctx, _colors.value.filterNot { it.id == id })

    // ---------- 导出 ----------
    fun exportJson(): String = json.encodeToString(serializer, _colors.value)

    suspend fun exportTo(ctx: Context, uri: android.net.Uri) = withContext(Dispatchers.IO) {
        ctx.contentResolver.openOutputStream(uri)!!.use { it.write(exportJson().toByteArray()) }
    }

    // ---------- 导入 ----------
    /** 解析任意宽松 JSON 颜色集合；返回新增数量 */
    fun parseAny(text: String): List<PaletteColor> {
        val root = json.parseToJsonElement(text)
        val arr: JsonArray = when (root) {
            is JsonArray -> root
            is JsonObject -> root["colors"]?.jsonArray
                ?: root.values.firstOrNull { it is JsonArray }?.jsonArray
                ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            fun pick(vararg keys: String): String? =
                keys.firstNotNullOfOrNull { k -> (o[k] as? JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content }
            val hexRaw = pick("hex", "color", "value", "rgb", "fill") ?: return@mapNotNull null
            val rgb = ColorMath.parseHex(hexRaw)
                ?: hexRaw.toIntOrNull()?.let { ColorMath.Rgb((it shr 16) and 255, (it shr 8) and 255, it and 255) }
                ?: return@mapNotNull null
            PaletteColor(
                name = pick("name", "title", "label") ?: rgb.toHex(),
                hex = rgb.toHex()
            )
        }
    }

    suspend fun importFrom(ctx: Context, text: String): Int {
        val parsed = parseAny(text)
        if (parsed.isEmpty()) return 0
        val existing = _colors.value.map { it.hex to it.name }.toSet()
        val fresh = parsed.filter { (it.hex to it.name) !in existing }
        commit(ctx, _colors.value + fresh)
        return fresh.size
    }

    suspend fun importFromFile(ctx: Context, uri: android.net.Uri): Int = withContext(Dispatchers.IO) {
        val text = ctx.contentResolver.openInputStream(uri)!!.bufferedReader().readText()
        importFrom(ctx, text)
    }

    suspend fun importFromUrl(ctx: Context, url: String): Int = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Accept", "application/json")
        try {
            require(conn.responseCode in 200..299) { "HTTP ${conn.responseCode}" }
            val text = conn.inputStream.bufferedReader().readText()
            importFrom(ctx, text)
        } finally {
            conn.disconnect()
        }
    }
}
