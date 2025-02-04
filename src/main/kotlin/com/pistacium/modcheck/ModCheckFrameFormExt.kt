package com.pistacium.modcheck

import com.pistacium.modcheck.util.*
import java.awt.*
import java.io.File
import java.net.URI
import java.nio.file.*
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.plaf.FontUIResource
import javax.swing.plaf.basic.BasicComboBoxEditor
import kotlin.io.path.*

class ModCheckFrameFormExt : ModCheckFrameForm() {
    private val modCheckBoxes = HashMap<Meta.Mod, JCheckBox>()
    private var currentOS: String = ModCheckUtils.currentOS()
    private var selectDirs: Array<File>? = null

    init {
        contentPane = mainPanel
        title = "ModCheck v" + ModCheck.applicationVersion + " by RedLime"
        setSize(1100, 700)
        isVisible = true
        setLocationRelativeTo(null)
        defaultCloseOperation = EXIT_ON_CLOSE

        font = FontUIResource("SansSerif", Font.BOLD, 13)
        UIManager.getLookAndFeel().defaults.forEach {
            if (it.value is Font) {
                UIManager.put(it.key, font)
            }
        }

        val resource = javaClass.classLoader.getResource("end_crystal.png")
        if (resource != null) iconImage = ImageIcon(resource).image

        initMenuBar()

        selectInstancePathsButton.addActionListener {
            val instanceDir = ModCheckUtils.readConfig()?.getDirectory()
            val pathSelector = JFileChooser(instanceDir?.toFile())
            pathSelector.isMultiSelectionEnabled = true
            pathSelector.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            pathSelector.dialogType = JFileChooser.CUSTOM_DIALOG
            pathSelector.dialogTitle = "Select Instance Paths"
            val jComboBox = SwingUtils.getDescendantsOfType(JComboBox::class.java, pathSelector).first()
            jComboBox.isEditable = true
            jComboBox.editor = object : BasicComboBoxEditor.UIResource() {
                override fun getItem(): Any {
                    return try {
                        File(super.getItem() as String)
                    } catch (e: Exception) {
                        super.getItem()
                    }
                }
            }

            val showDialog = pathSelector.showDialog(this, "Select")
            val instanceDirectories = pathSelector.selectedFiles
            if (instanceDirectories != null && showDialog == JFileChooser.APPROVE_OPTION) {
                selectDirs = instanceDirectories
                var parentDir = ""
                val stringBuilder = StringBuilder()
                for (selectDir in instanceDirectories.slice(IntRange(0, instanceDirectories.size.coerceAtMost(4) - 1))) {
                    stringBuilder.append(if (parentDir.isEmpty()) selectDir.path else selectDir.path.replace(parentDir, "")).append(", ")
                    parentDir = selectDir.parent
                }
                if (instanceDirectories.size > 4) {
                    stringBuilder.append("and " + (instanceDirectories.size - 4) + " more...")
                }
                selectedDirLabel.text =
                    "<html>Selected Instances: " + stringBuilder.removeSuffix(", ") + "</html>"
            }
            if (instanceDirectories.isNotEmpty()) {
                ModCheckUtils.writeConfig(instanceDirectories[0].parentFile.toPath())
            }
        }

        progressBar.string = "Idle..."
        downloadButton.addActionListener {
            if (selectDirs == null || selectDirs!!.isEmpty()) {
                return@addActionListener
            }

            if (mcVersionCombo.selectedItem == null) {
                JOptionPane.showMessageDialog(this, "Error: selected item is null")
                downloadButton.isEnabled = true
                return@addActionListener
            }

            downloadButton.isEnabled = false
            val modsFileStack = Stack<Path>()

            var overrideDownloadChecks = false

            for (instanceDir in selectDirs!!) {
                var instancePath = instanceDir.toPath()

                // if mods is selected, we don't have to do anything
                if (instancePath.fileName.toString() == "mods" && Files.isDirectory(instancePath) && instancePath.parent.fileName.toString() in listOf(".minecraft", "minecraft")) {
                    modsFileStack.push(instancePath)
                    continue
                }

                if (Files.isDirectory(instancePath.resolve(".minecraft"))) {
                    instancePath = instancePath.resolve(".minecraft")
                } else if (Files.isDirectory(instancePath.resolve("minecraft"))) {
                    instancePath = instancePath.resolve("minecraft")
                } else if (Files.exists(instancePath.resolve("mmc-pack.json")) || Files.exists(instancePath.resolve("instance.cfg"))) {
                    // when adding a new instance in multimc / prism,
                    // right away only mmc-pack.json & instance.cfg get created
                    instancePath = Files.createDirectory(instancePath.resolve(".minecraft"))
                    println("Created '.minecraft' folder for multimc: $instancePath")
                }

                if (instancePath.fileName.toString() !in listOf(".minecraft", "minecraft") && !overrideDownloadChecks) {
                    if (JOptionPane.showConfirmDialog(
                            this,
                            "You have selected a directory but not a Minecraft instance directory.\nAre you sure you want to download in this directory?",
                            "Wrong instance directory",
                            JOptionPane.OK_CANCEL_OPTION
                        ) == JOptionPane.CANCEL_OPTION
                    ) {
                        downloadButton.isEnabled = true
                        return@addActionListener
                    } else {
                        overrideDownloadChecks = true
                    }
                }

                val modsPath = instancePath.resolve("mods")
                if (!Files.exists(modsPath)) {
                    println("Created 'mods' folder: $modsPath")
                    Files.createDirectory(modsPath)
                }
                modsFileStack.push(modsPath)
            }

            val targetMods = ArrayList<Meta.Mod>()
            var maxCount = 0
            for ((key, value) in modCheckBoxes) {
                if (value.isSelected && value.isEnabled) {
                    println("Selected " + key.name)
                    targetMods.add(key)
                    maxCount++
                }
            }
            val minecraftVersion = mcVersionCombo.selectedItem as String

            for (instanceDir in modsFileStack) {
                val modFiles = Files.list(instanceDir) ?: return@addActionListener
                for (file in modFiles) {
                    if (file.name.endsWith(".jar")) {
                        if (deleteAllJarCheckbox.isSelected) {
                            Files.deleteIfExists(file)
                        } else {
                            val modName = file.name.split("-").first().split("+").first()
                            for (targetMod in targetMods) {
                                val targetModFileName = targetMod.getModVersion(minecraftVersion)?.url?.substringAfterLast("/")
                                if (targetModFileName?.startsWith(modName) == true) {
                                    Files.deleteIfExists(file)
                                }
                            }
                        }
                    }
                    if (file.name.lowercase().contains("serversiderng")) {
                        askDeleteSSRNG(file)
                        continue
                    }
                }
            }

            progressBar.value = 0
            ModCheck.setStatus(ModCheckStatus.DOWNLOADING_MOD_FILE)

            val finalMaxCount = maxCount
            ModCheck.threadExecutor.submit {
                val failedMods = ArrayList<Meta.Mod>()
                for ((count, targetMod) in targetMods.withIndex()) {
                    progressBar.string = "Downloading " + targetMod.name
                    println("Downloading " + targetMod.name)
                    val downloadFiles = Stack<Path>()
                    downloadFiles.addAll(modsFileStack)
                    if (!downloadFile(minecraftVersion, targetMod, downloadFiles)) {
                        println("Failed to download " + targetMod.name)
                        failedMods.add(targetMod)
                    }
                    progressBar.value = (((count + 1) / (finalMaxCount * 1f)) * 100).toInt()
                }
                progressBar.value = 100
                ModCheck.setStatus(ModCheckStatus.IDLE)

                println("Downloading mods complete")

                if (failedMods.isNotEmpty()) {
                    val failedModString = StringBuilder()
                    for (failedMod in failedMods) {
                        failedModString.append(failedMod.name).append(", ")
                    }
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to download " + failedModString.substring(0, failedModString.length - 2) + ".",
                        "Please try again",
                        JOptionPane.ERROR_MESSAGE
                    )
                } else {
                    JOptionPane.showMessageDialog(this, "All selected mods have been downloaded!")
                }
                downloadButton.isEnabled = true
            }
        }
        downloadButton.isEnabled = false

        mcVersionCombo.addActionListener { updateModList() }

        selectAllRecommendsButton.addActionListener {
            for ((key, value) in modCheckBoxes) {
                if (!key.recommended || !key.getModVersion(mcVersionCombo.selectedItem as String)!!.recommended
                    || key.incompatibilities.stream().anyMatch { incompatible: String ->
                        modCheckBoxes.entries.stream().anyMatch { entry2: Map.Entry<Meta.Mod, JCheckBox> -> entry2.key.name == incompatible && entry2.value.isSelected }
                    }
                ) continue

                if (value.isEnabled) {
                    value.isSelected = true
                }
            }
            // JOptionPane.showMessageDialog(
            //     this,
            //     "<html><body>Some mods that have warnings (like noPeaceful)<br> or incompatible with other mods (like Starlight and Phosphor) aren't automatically selected.<br>You have to select them yourself.</body></html>",
            //     "WARNING!",
            //     JOptionPane.WARNING_MESSAGE
            // )
        }

        deselectAllButton.addActionListener {
            for (cb in modCheckBoxes.values) {
                cb.isSelected = false
                cb.isEnabled = true
            }
        }

        // TODO: enumification
        windowsRadioButton.addActionListener {
            currentOS = "windows"
            updateModList()
        }
        if (currentOS == "windows") windowsRadioButton.isSelected = true
        macRadioButton.addActionListener {
            currentOS = "osx"
            updateModList()
        }
        if (currentOS == "osx") macRadioButton.isSelected = true
        linuxRadioButton.addActionListener {
            currentOS = "linux"
            updateModList()
        }
        if (currentOS == "linux") linuxRadioButton.isSelected = true

        randomSeedRadioButton.addActionListener { updateModList() }
        setSeedRadioButton.addActionListener { updateModList() }
        accessibilityCheckBox.addActionListener {
            if (accessibilityCheckBox.isSelected) {
                val message = "These mods have consequences to legality and possibility of rejection if used improperly, please make yourself familiar with the rules about each one if you choose to use them."
                val result = JOptionPane.showConfirmDialog(this, message, "THIS OPTION IS NOT FOR ALL!", JOptionPane.OK_CANCEL_OPTION)
                if (result == 0) {
                    updateModList()
                } else {
                    accessibilityCheckBox.isSelected = false
                }
            } else {
                updateModList()
            }
        }
        obsoleteCheckBox.addActionListener { updateModList() }

        updateExistingModsButton.addActionListener {
            if (selectDirs == null || selectDirs!!.isEmpty()) {
                return@addActionListener
            }
            progressBar.value = 0
            ModCheck.setStatus(ModCheckStatus.GETTING_INSTALLED_MODS)
            val resolvedModFolders = ArrayList<Path>()
            for (dir in selectDirs!!) {
                var modFolder = dir.toPath()
                // support for selecting either the outer mmc dir or .minecraft
                if (modFolder.isDirectory() && modFolder.fileName.toString() == "mods" && modFolder.parent.name in arrayOf("minecraft", ".minecraft")) {
                    resolvedModFolders.add(modFolder)
                    continue
                } else if (modFolder.resolve(".minecraft").isDirectory()) {
                    modFolder = modFolder.resolve(".minecraft")
                } else if (modFolder.resolve("minecraft").isDirectory()) {
                    modFolder = modFolder.resolve("minecraft")
                }
                modFolder = modFolder.resolve("mods")
                // no mod folder, no service
                if (modFolder.isDirectory()) {
                    resolvedModFolders.add(modFolder)
                }
            }
            ModCheck.setStatus(ModCheckStatus.DOWNLOADING_MOD_FILE)
            for (modFolder in resolvedModFolders) {
                // store the file to be replaced, so we don't have to guess the name via string splicing
                val replacedJars = ArrayList<Pair<Path, Meta.ModVersion>>()
                for (modJar in modFolder.listDirectoryEntries()) {
                    // retain .disabled mods
                    if (modJar.extension != "jar") continue
                    val fmj = ModCheckUtils.readFabricModJson(modJar) ?: continue

                    if (fmj.id == "serversiderng") {
                        askDeleteSSRNG(modJar)
                        continue
                    }
                    // if mod id is in available mods, find the newest version for the selected version
                    // we can't get the minecraft version from the instance itself easily while still supporting vanilla launcher
                    val modVersion = ModCheck.availableMods.find { it.modid == fmj.id || it.name == fmj.name }?.getModVersion(mcVersionCombo.selectedItem as String) ?: continue
                    if (modVersion.version != fmj.version) {
                        replacedJars.add(Pair(modJar, modVersion))
                    }
                }
                var count = 0
                for (newMod in replacedJars) {
                    // requires that the new mod and old mod have different filenames
                    if (downloadFile(newMod.second, modFolder)) {
                        Files.deleteIfExists(newMod.first)
                    }
                    count++
                    progressBar.value = count / replacedJars.size
                }
            }
            progressBar.value = 100
            ModCheck.setStatus(ModCheckStatus.IDLE)
            JOptionPane.showMessageDialog(this, "Known mods have been updated!")
        }
    }

    private fun downloadFile(minecraftVersion: String, targetMod: Meta.Mod, instances: Stack<Path>): Boolean {
        val url = targetMod.getModVersion(minecraftVersion)?.url ?: return false
        val filename = url.substringAfterLast("/")
        val bytes = URI.create(url).toURL().readBytes()
        instances.forEach {
            it.resolve(filename).writeBytes(bytes)
        }
        return true
    }

    // TODO: bytearr caching
    private fun downloadFile(modVersion: Meta.ModVersion, instance: Path): Boolean {
        val url = modVersion.url
        val filename = url.substringAfterLast("/")
        val bytes = URI.create(url).toURL().readBytes()
        instance.resolve(filename).writeBytes(bytes)
        return true
    }


    private fun initMenuBar() {
        val menuBar = JMenuBar()

        val source = JMenu("Info")

        val githubSource = JMenuItem("GitHub...")
        githubSource.addActionListener {
            try {
                Desktop.getDesktop().browse(URI("https://github.com/tildejustin/modcheck"))
            } catch (ignored: Exception) {
            }
        }
        source.add(githubSource)

        val donateSource = JMenuItem("Support")
        donateSource.addActionListener {
            try {
                Desktop.getDesktop().browse(URI("https://ko-fi.com/redlimerl"))
            } catch (ignored: Exception) {
            }
        }
        source.add(donateSource)

        val checkChangeLogSource = JMenuItem("Changelog")
        checkChangeLogSource.addActionListener {
            try {
                Desktop.getDesktop().browse(URI("https://github.com/tildejustin/modcheck/releases/tag/" + ModCheck.applicationVersion))
            } catch (ignored: Exception) {
            }
        }
        source.add(checkChangeLogSource)

        val updateCheckSource = JMenuItem("Check for updates")
        updateCheckSource.addActionListener(ModCheckUtils::checkForUpdates)
        source.add(updateCheckSource)

        menuBar.add(source)

        this.jMenuBar = menuBar
    }

    fun updateVersionList() {
        mcVersionCombo.removeAllItems()
        for (availableVersion in ModCheck.availableVersions) {
            mcVersionCombo.addItem(availableVersion)
        }
        mcVersionCombo.selectedItem = ModCheck.availableVersions.first()
        updateModList()
    }


    private fun updateModList() {
        modListPanel.removeAll()
        modListPanel.layout = BoxLayout(modListPanel, BoxLayout.Y_AXIS)
        modCheckBoxes.clear()

        if (mcVersionCombo.selectedItem == null) return

        val mcVersion: String = mcVersionCombo.selectedItem as String

        outer@ for (mod in ModCheck.availableMods) {
            val modVersion = mod.getModVersion(mcVersion)
            if (modVersion != null) {
                // prioritize sodium-mac
                if (mod.modid == "sodium" && currentOS == "osx" && ModCheck.availableMods.find { it.modid == "sodiummac" }?.versions?.find { it.target_version.contains(mcVersion) } != null) continue@outer
                if ((mod.obsolete || modVersion.obsolete) && !obsoleteCheckBox.isSelected) continue@outer
                for (condition in mod.traits) {
                    if (condition == "ssg-only" && !setSeedRadioButton.isSelected) continue@outer
                    if (condition == "rsg-only" && !randomSeedRadioButton.isSelected) continue@outer
                    if (condition == "accessibility" && !accessibilityCheckBox.isSelected) continue@outer
                    if (condition == "mac-only" && currentOS != "osx") continue@outer
                }

                val modPanel = JPanel()
                modPanel.layout = BoxLayout(modPanel, BoxLayout.Y_AXIS)

                val versionName: String = modVersion.version
                val checkBox = JCheckBox(mod.name + " (" + versionName + ")")
                checkBox.addChangeListener {
                    modCheckBoxes.entries.stream()
                        .filter {
                            it.key.incompatibilities.contains(mod.modid) || mod.incompatibilities.contains(it.key.modid)
                        }
                        .forEach { entry ->
                            entry.value.setEnabled(
                                modCheckBoxes.entries.stream()
                                    .noneMatch {
                                        it.key.incompatibilities
                                            .contains(it.key.modid) || it.key.incompatibilities.contains(entry.key.modid) && it.value.isSelected
                                    }
                            )
                        }
                }

                val line: Int = mod.description.split("\n").size
                val description =
                    JLabel("<html><body>" + mod.description.replace("\n", "<br>").replace("<a ", "<b ").replace("</a>", "</b>") + "</body></html>")
                description.maximumSize = Dimension(800, 70 * line)
                description.border = EmptyBorder(0, 15, 0, 0)
                val f = description.font
                description.font = f.deriveFont(f.style and Font.BOLD.inv())

                modPanel.add(checkBox)
                modPanel.add(description)
                modPanel.maximumSize = Dimension(950, 60 * line)
                modPanel.border = EmptyBorder(0, 10, 10, 0)

                modListPanel.add(modPanel)
                modCheckBoxes[mod] = checkBox
            }
        }
        modListPanel.updateUI()
        modListScroll.updateUI()
        downloadButton.isEnabled = true
    }

    private fun askDeleteSSRNG(file: Path) {
        val shouldDelete = JOptionPane.showConfirmDialog(
            null,
            "ServerSideRNG has been detected in your mod folder. As the mod is now illegal, would you like it to be deleted?",
            UIManager.getString("OptionPane.titleText"),
            JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION
        if (shouldDelete) {
            Files.deleteIfExists(file)
        }
    }
}
