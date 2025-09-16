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
        // enable cli if arguments are given
        if (args.isNotEmpty()) {
            handleCliMode(args)
            return
        }
        
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
        // Load mod list for CLI
        val mods = ModCheckUtils.json.decodeFromString<Meta>(
            URI.create("https://raw.githubusercontent.com/tildejustin/mcsr-meta/${if (applicationVersion == "dev") "staging" else "schema-6"}/mods.json").toURL().readText()
        ).mods
        availableMods.clear()
        availableMods.addAll(mods)
        var path: String? = null
        var instance: String? = null
        var function: String? = null

        // Defaults
        var category = "rsg"
        var currentOS: String = ModCheckUtils.currentOS()
        var os = currentOS
    var accessibility = false
    var version = "1.16.1"


        // Parsing args
        var i = 0
        while (i < args.size) {
            when (args[i].lowercase()) {
                "help", "-h", "--help" -> {
                    printHelp()
                    return
                }
                "version", "-v", -> {
                    println("ModCheck version: $applicationVersion")
                    return
                }
                "--category" -> {
                    if (i + 1 < args.size) {
                        val value = args[i + 1].lowercase()
                        if (value == "rsg" || value == "ssg") {
                            category = value
                        } else {
                            println("Invalid category: $value")
                            exitProcess(1)
                        }
                        i++
                    }
                }
                "--accessibility" -> {
                    accessibility = true
                }
                "--version" -> {
                    if (i + 1 < args.size) {
                        version = args[i + 1]
                        i++
                    }
                }
                "--path" -> {
                    if (i + 1 < args.size) {
                        path = args[i + 1]
                        i++
                    }
                }
                "--instance" -> {
                    if (i + 1 < args.size) {
                        instance = args[i + 1]
                        i++
                    }
                }
                "download", "update" -> {
                    function = args[i].lowercase()
                }
                else -> {
                    // edge cases?
                }
            }
            i++
        }

        if (instance != null && path == null) {
            val userHome = System.getProperty("user.home")
            path = when {
                os == "windows" -> {
                    println("Error: --instance is not supported on Windows. Please use --path <directory> instead.")
                    exitProcess(1)
                }
                os == "linux" -> "$userHome/.local/share/PrismLauncher/instances/$instance"
                os == "osx" -> "$userHome/Library/Application Support/PrismLauncher/instances/$instance"
                else -> {
                    println("Unknown OS for --instance path resolution: $os")
                    exitProcess(1)
                }
            }
        }

        if (function == null) {
            println("Usage: java -jar modcheck.jar [options] download|update")
            exitProcess(1)
        }

        if (path == null) {
            println("Error: Either --path <directory> or --instance <name> is required.")
            println("Usage: java -jar modcheck.jar [options] --path <directory>|--instance <name> download|update")
            exitProcess(1)
        }

        // Print out the values
    // Determine modsDir for debug print
    val basePath = java.nio.file.Paths.get(path)
    val mcPath = basePath.resolve("minecraft")
    val dotMcPath = basePath.resolve(".minecraft")
    val modsDirDebug = when {
        java.nio.file.Files.isDirectory(mcPath) && java.nio.file.Files.isDirectory(mcPath.resolve("mods")) -> mcPath.resolve("mods")
        java.nio.file.Files.isDirectory(dotMcPath) && java.nio.file.Files.isDirectory(dotMcPath.resolve("mods")) -> dotMcPath.resolve("mods")
        java.nio.file.Files.isDirectory(basePath.resolve("mods")) -> basePath.resolve("mods")
        else -> null
    }
    println("Options:")
    println("  Category: ${if (category == "rsg") "Random Seed" else "Set Seed"}")
    println("  OS: ${os.replaceFirstChar { it.uppercase() }}")
    println("  Accessibility: $accessibility")
    println("  Version: $version")
    println("  Instance Path: $path")

        if (function == "download") {
            // Download logic with debug prints
            // 1. Select mods
            val basePath = java.nio.file.Paths.get(path)
            val mcPath = basePath.resolve("minecraft")
            val dotMcPath = basePath.resolve(".minecraft")
            val modsDir = when {
                java.nio.file.Files.isDirectory(mcPath) && java.nio.file.Files.isDirectory(mcPath.resolve("mods")) -> mcPath.resolve("mods")
                java.nio.file.Files.isDirectory(dotMcPath) && java.nio.file.Files.isDirectory(dotMcPath.resolve("mods")) -> dotMcPath.resolve("mods")
                java.nio.file.Files.isDirectory(basePath.resolve("mods")) -> basePath.resolve("mods")
                else -> null
            }
            if (modsDir == null) {
                println("No mods directory found at: $modsDir")
                return
            }
            val selectedMods = availableMods.filter { mod ->
                val modVersion = mod.getModVersion(version)
                if (modVersion == null) return@filter false
                // prioritize sodium-mac
                if (
                    mod.modid == "sodium" &&
                    os == "osx" &&
                    availableMods.find { it.modid == "sodiummac" }
                        ?.versions?.any { version in it.target_version } == true
                ) return@filter false
                for (trait in mod.traits) {
                    if (trait == "ssg-only" && category != "ssg") return@filter false
                    if (trait == "rsg-only" && category != "rsg") return@filter false
                    if (trait == "accessibility" && !accessibility) return@filter false
                    if (trait == "mac-only" && os != "osx") return@filter false
                }
                true
            }
            println("Selected mods count: ${selectedMods.size}")
            if (selectedMods.isEmpty()) {
                println("Warning: No mods matched the selection criteria. Nothing to download.")
                return
            } else {
                for (mod in selectedMods) {
                    println("Selected ${mod.name}")
                }
            }
            // 2. Download selected mods
            var count = 0
            for (mod in selectedMods) {
                val modVersion = mod.getModVersion(version)
                if (modVersion == null) {
                    println("Skipping ${mod.name}: no version for $version")
                    continue
                }
                val url = modVersion.url
                val filename = url.substringAfterLast("/")
                try {
                    println("Downloading ${mod.name}")
                    val bytes = java.net.URI.create(url).toURL().readBytes()
                    java.nio.file.Files.write(modsDir.resolve(filename), bytes)
                } catch (e: Exception) {
                    println("Failed to download ${mod.name}: ${e.message}")
                }
                count++
            }
            println("Downloading mods complete")
        } else if (function == "update") {
            // 1. Find installed mods
            val basePath = java.nio.file.Paths.get(path)
            val mcPath = basePath.resolve("minecraft")
            val dotMcPath = basePath.resolve(".minecraft")
            val modsDir = when {
                java.nio.file.Files.isDirectory(mcPath) && java.nio.file.Files.isDirectory(mcPath.resolve("mods")) -> mcPath.resolve("mods")
                java.nio.file.Files.isDirectory(dotMcPath) && java.nio.file.Files.isDirectory(dotMcPath.resolve("mods")) -> dotMcPath.resolve("mods")
                java.nio.file.Files.isDirectory(basePath.resolve("mods")) -> basePath.resolve("mods")
                else -> null
            }
            if (modsDir == null || !java.nio.file.Files.exists(modsDir)) {
                println("No mods directory found at: $modsDir")
                return
            }
            println("Resolved mods directory: ${modsDir.toAbsolutePath()}")
            val modFiles = java.nio.file.Files.list(modsDir).use { it.iterator().asSequence().toList() }
            val toUpdate = mutableListOf<Triple<java.nio.file.Path, Meta.Mod, Meta.ModVersion>>()
            for (file in modFiles) {
                if (!file.fileName.toString().endsWith(".jar")) continue
                val fmj = try { com.pistacium.modcheck.util.ModCheckUtils.readFabricModJson(file) } catch (_: Exception) { null }
                if (fmj == null) continue
                if (fmj.id == "serversiderng") {
                    println("Deleting illegal mod: SSRNG (${file.fileName})")
                    java.nio.file.Files.deleteIfExists(file)
                    continue
                }
                val mod = availableMods.find { it.modid == fmj.id || it.name == fmj.name }
                if (mod != null) {
                    val modVersion = mod.getModVersion(version)
                    if (modVersion != null && modVersion.version != fmj.version) {
                        toUpdate.add(Triple(file, mod, modVersion))
                    }
                }
            }
            if (toUpdate.isEmpty()) {
                println("All known mods are up to date.")
                return
            }
            var count = 0
            for ((oldFile, mod, modVersion) in toUpdate) {
                val url = modVersion.url
                val filename = url.substringAfterLast("/")
                try {
                    println("Updating ${mod.name} from ${oldFile.fileName} to $filename")
                    val bytes = java.net.URI.create(url).toURL().readBytes()
                    java.nio.file.Files.write(modsDir.resolve(filename), bytes)
                    java.nio.file.Files.deleteIfExists(oldFile)
                } catch (e: Exception) {
                    println("Failed to update ${mod.name}: ${e.message}")
                }
                count++
            }
            println("Updated $count mod(s). Done.")
        }
    }

    private fun printHelp() {
        println("""
            ModCheck CLI

            Usage: java -jar modcheck.jar [options] <download|update>


            Options:
                --category <rsg|ssg>        Specify the category (default: rsg)
                --version <version>         Specify Minecraft version (default: 1.16.1)
                --accessibility             Include accessibility mods (default: false)
                --instance <name>           Specify your instance name (uses default PrismLauncher path)
                    or
                --path <directory>          Specify a different path to your instance

                help, -h, --help            Show this help message
                version, -v                 Show modcheck version information

            Commands:
                download                     Download mods
                update                       Update existing mods

            Examples:
                java -jar modcheck.jar --category random --version 1.16.1 --instance instance1 download
                    Downloads speedrunning mods for 1.16.1 RSG into instance1

            Run without arguments to start the GUI.
        """.trimIndent())
    }
}
