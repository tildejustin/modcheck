package com.pistacium.modcheck

import com.formdev.flatlaf.FlatDarkLaf
import com.pistacium.modcheck.util.ModCheckStatus
import kotlinx.serialization.json.Json
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.*
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.*
import javax.swing.JOptionPane
import kotlin.io.path.readText
import kotlin.system.exitProcess

object ModCheck {
    fun setStatus(status: ModCheckStatus) {
        frameInstance.progressBar?.string = status.description
    }

    val threadExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    lateinit var frameInstance: ModCheckFrameFormExt

    lateinit var availableVersions: MutableList<String>

    val availableMods: ArrayList<Meta.Mod> = ArrayList()

    const val applicationVersion: String = "2.0"

    @JvmStatic
    fun main(args: Array<String>) {
        FlatDarkLaf.setup()
        threadExecutor.submit {
            try {
                frameInstance = ModCheckFrameFormExt()

                // Get available versions
                setStatus(ModCheckStatus.LOADING_AVAILABLE_VERSIONS)
                availableVersions = Json.decodeFromString(URI.create("https://raw.githubusercontent.com/tildejustin/mcsr-meta/main/important_versions.json").toURL().readText())

                // Get mod list
                setStatus(ModCheckStatus.LOADING_MOD_LIST)
                val mods = Json.decodeFromString<Meta>(URI.create("https://raw.githubusercontent.com/tildejustin/mcsr-meta/main/mods.json").toURL().readText()).mods
                frameInstance.progressBar?.value = 60

                setStatus(ModCheckStatus.LOADING_MOD_RESOURCE)
                val maxCount = mods.count()
                for ((count, mod) in mods.withIndex()) {
                    frameInstance.progressBar?.string = "Loading information of " + mod.name
                    availableMods.add(mod)
                    frameInstance.progressBar?.value = (60 + ((((count + 1) * 1) / maxCount) * 40))
                }
                frameInstance.progressBar?.value = 100
                setStatus(ModCheckStatus.IDLE)
                frameInstance.updateVersionList()
            } catch (e: Throwable) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                val result = JOptionPane.showOptionDialog(
                    null,
                    sw.toString(),
                    "Error exception!",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    arrayOf("Copy to clipboard a logs", "Cancel"),
                    "Copy to clipboard a logs"
                )
                if (result == 0) {
                    val selection = StringSelection(sw.toString())
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(selection, selection)
                }

                exitProcess(0)
            }
        }
        // System.out.println(new Gson().toJson(ModCheckUtils.getFabricJsonFileInJar(new File("D:/MultiMC/instances/1.16-1/.minecraft/mods/SpeedRunIGT-10.0+1.16.1.jar"))));
    }
}