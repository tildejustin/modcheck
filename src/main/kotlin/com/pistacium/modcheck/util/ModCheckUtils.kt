package com.pistacium.modcheck.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.*
import java.net.*
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.util.*
import kotlin.io.path.*

object ModCheckUtils {
    private val urlReqCache = HashMap<String, String>()
    private val config: Path = Path.of("modcheck.json")

    fun getUrlRequest(url: String): String? {
        if (urlReqCache.containsKey(url)) {
            return urlReqCache[url]
        }
        try {
            val obj = URI.create(url).toURL()
            val conn = obj.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val br = BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8))

            var inputLine: String?
            val response = StringBuilder()

            while ((br.readLine().also { inputLine = it }) != null) {
                response.append(inputLine)
            }
            br.close()

            urlReqCache[url] = response.toString()
            return response.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        throw IllegalAccessException("Couldn't loading url request data! check your internet status")
    }

    fun readConfig(): Config? {
        if (!Files.exists(config)) {
            return null
        }
        return Json.decodeFromString<Config>(config.readText())
    }

    fun writeConfig(dir: Path) {
        val config = Config(dir.toString())
        this.config.writeText(Json.encodeToString(config))
    }

    fun currentOS(): String {
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        if (osName.contains("win")) return "windows"
        if (osName.contains("mac")) return "osx"
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return "linux"
        }
        return "unknown"
    }

    fun latestVersion(): String? {
        val urlRequest = getUrlRequest("https://api.github.com/repos/RedLime/ModCheck/releases/latest") ?: return null
        val jsonObject = Json.parseToJsonElement(urlRequest).jsonObject
        val newVersion = jsonObject["tag_name"]?.jsonPrimitive?.content ?: return null
        return newVersion
    }
}