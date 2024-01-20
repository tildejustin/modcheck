package com.pistacium.modcheck.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.URI
import java.nio.file.*
import java.util.*
import kotlin.io.path.*

object ModCheckUtils {
    private val config: Path = Path.of("modcheck.json")

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
        val urlRequest = URI.create("https://api.github.com/repos/RedLime/ModCheck/releases/latest").toURL().readText()
        val jsonObject = Json.parseToJsonElement(urlRequest).jsonObject
        val newVersion = jsonObject["tag_name"]?.jsonPrimitive?.content ?: return null
        return newVersion
    }
}