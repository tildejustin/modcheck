package com.pistacium.modcheck

import com.formdev.flatlaf.FlatDarkLaf
import com.pistacium.modcheck.util.*
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.*
import java.net.URI
import java.util.concurrent.*
import javax.swing.JOptionPane
import kotlin.system.exitProcess

object ModCheck {
    fun setStatus(status: ModCheckStatus) {
        frameInstance.progressBar?.string = status.description
    }

    val threadExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    lateinit var frameInstance: ModCheckFrameFormExt

    lateinit var availableVersions: MutableList<String>

    val availableMods: ArrayList<Meta.Mod> = ArrayList()

    val applicationVersion: String = ModCheck.javaClass.`package`.implementationVersion ?: "dev"

    @JvmStatic
    fun main(args: Array<String>) {
        // Check for CLI mode
        if (args.isNotEmpty()) {
            handleCliMode(args)
            return
        }
        
        // GUI mode (original behavior)
        FlatDarkLaf.setup()
        threadExecutor.submit {
            try {
                frameInstance = ModCheckFrameFormExt()

                // Get available versions
                setStatus(ModCheckStatus.LOADING_AVAILABLE_VERSIONS)
                availableVersions = ModCheckUtils.json.decodeFromString(
                    URI.create("https://raw.githubusercontent.com/tildejustin/mcsr-meta/${if (applicationVersion == "dev") "staging" else "schema-6"}/important_versions.json").toURL()
                        .readText()
                )

                // Get mod list
                setStatus(ModCheckStatus.LOADING_MOD_LIST)
                val mods = ModCheckUtils.json.decodeFromString<Meta>(
                    URI.create("https://raw.githubusercontent.com/tildejustin/mcsr-meta/${if (applicationVersion == "dev") "staging" else "schema-6"}/mods.json").toURL().readText()
                ).mods
                // val mods = Json.decodeFromString<Meta>(Path.of("/home/justin/IdeaProjects/mcsr-meta/mods.json").readText()).mods
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
                ModCheckUtils.checkForUpdates(false)
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
    }

    private fun handleCliMode(args: Array<String>) {
        when (args[0].lowercase()) {
            "help", "-h", "--help" -> {
                printHelp()
            }
            "version", "-v", "--version" -> {
                println("ModCheck version: $applicationVersion")
            }
            else -> {
                println("Unknown command: ${args[0]}")
                println("Use 'help' to see available commands.")
                exitProcess(1)
            }
        }
    }

    private fun printHelp() {
        println("""
            ModCheck CLI
            
            Usage: java -jar modcheck.jar [command] [options]
            
            Commands:
              help, -h, --help     Show this help message
              version, -v, --version  Show version information
            
            Examples:
              ...
            
            Run without arguments to start the GUI.
        """.trimIndent())
    }
}
