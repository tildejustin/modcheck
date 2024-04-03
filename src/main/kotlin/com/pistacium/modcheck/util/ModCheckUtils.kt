package com.pistacium.modcheck.util

import com.pistacium.modcheck.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.awt.Desktop
import java.awt.event.ActionEvent
import java.net.URI
import java.nio.file.*
import java.util.*
import javax.swing.JOptionPane
import kotlin.io.path.*

object ModCheckUtils {
    private val config: Path = Paths.get("modcheck.json")

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
        val urlRequest = URI.create("https://api.github.com/repos/tildejustin/modcheck/releases/latest").toURL().readText()
        val jsonObject = Json.parseToJsonElement(urlRequest).jsonObject
        val newVersion = jsonObject["tag_name"]?.jsonPrimitive?.content ?: return null
        return newVersion
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; prettyPrintIndent = "  " }

    fun readFabricModJson(mod: Path): FabricModJson? {
        FileSystems.newFileSystem(mod, null as ClassLoader?).use { fs ->
            val jsonFilePath = fs.getPath("fabric.mod.json")
            val jsonData: ByteArray
            try {
                jsonData = Files.readAllBytes(jsonFilePath)
            } catch (e: NoSuchFileException) {
                return null
            }
            return json.decodeFromString<FabricModJson>(String(jsonData))
        }
    }

    fun checkForUpdates(e: ActionEvent) {
        checkForUpdates(true)
    }

    fun checkForUpdates(verbose: Boolean) {
        val latestVersion = latestVersion()
        if (latestVersion != null && latestVersion > ModCheck.applicationVersion) {
            val result = JOptionPane.showOptionDialog(
                null,
                "<html><body>Found new ModCheck update!<br><br>Current Version : " + ModCheck.applicationVersion + "<br>Updated Version : " + latestVersion + "</body></html>",
                "Update Checker",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                arrayOf("Download", "Cancel"),
                "Download"
            )
            if (result == 0) {
                Desktop.getDesktop().browse(URI.create("https://github.com/tildejustin/modcheck/releases/latest"))
            }
        } else if (verbose) {
            JOptionPane.showMessageDialog(ModCheck.frameInstance, "You are using the latest version!")
        }
    }
}