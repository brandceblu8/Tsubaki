package com.ncclab.tsubaki.data.model

/**
 * 对扫描到的原始字符串做语义化解析，便于 UI 决定展示哪种交互。
 */
sealed class ParsedPayload {
    data class Url(val url: String) : ParsedPayload()

    data class Wifi(
        val ssid: String,
        val password: String,
        val security: Security,
        val hidden: Boolean
    ) : ParsedPayload() {
        enum class Security { WPA, WEP, NONE }
    }

    data class PlainText(val text: String) : ParsedPayload()
}

object PayloadParser {

    fun parse(raw: String): ParsedPayload {
        val trimmed = raw.trim()

        // Wi-Fi: 标准格式  WIFI:T:WPA;S:mynet;P:mypass;H:false;;
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            parseWifi(trimmed)?.let { return it }
        }

        if (looksLikeUrl(trimmed)) {
            return ParsedPayload.Url(trimmed)
        }

        return ParsedPayload.PlainText(raw)
    }

    /**
     * 判断是否为可被 Intent.ACTION_VIEW 处理的 URI。除常见 http/https 外，
     * 也包括 weixin://、mqqapi://、alipays:// 等 scheme，让系统弹出应用选择器
     * 而不是直接跳浏览器。
     */
    private fun looksLikeUrl(s: String): Boolean {
        if (s.isEmpty() || s.contains('\n') || s.contains(' ')) return false
        // scheme://something
        val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.+")
        if (schemeRegex.matches(s)) return true
        // 没有协议头但形如 example.com/...
        val hostRegex = Regex("^([\\w-]+\\.)+[a-zA-Z]{2,}(/.*)?$")
        return hostRegex.matches(s)
    }

    private fun parseWifi(s: String): ParsedPayload.Wifi? {
        // 去掉前缀 "WIFI:"
        val body = s.substring(5).trimEnd(';')
        val fields = mutableMapOf<String, String>()

        // 按未转义的分号切分
        val sb = StringBuilder()
        var i = 0
        val parts = mutableListOf<String>()
        while (i < body.length) {
            val c = body[i]
            if (c == '\\' && i + 1 < body.length) {
                sb.append(body[i + 1])
                i += 2
                continue
            }
            if (c == ';') {
                parts.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty()) parts.add(sb.toString())

        for (part in parts) {
            val idx = part.indexOf(':')
            if (idx <= 0) continue
            val key = part.substring(0, idx).uppercase()
            val value = part.substring(idx + 1)
            fields[key] = value
        }

        val ssid = fields["S"].orEmpty()
        if (ssid.isEmpty()) return null
        val password = fields["P"].orEmpty()
        val security = when (fields["T"]?.uppercase()) {
            "WPA", "WPA2", "WPA3", "WPA/WPA2", "SAE" -> ParsedPayload.Wifi.Security.WPA
            "WEP" -> ParsedPayload.Wifi.Security.WEP
            "NOPASS", "", null -> ParsedPayload.Wifi.Security.NONE
            else -> ParsedPayload.Wifi.Security.WPA
        }
        val hidden = fields["H"]?.equals("true", ignoreCase = true) == true
        return ParsedPayload.Wifi(ssid, password, security, hidden)
    }
}
