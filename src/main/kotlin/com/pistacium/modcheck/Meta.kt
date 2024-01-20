package com.pistacium.modcheck

import kotlinx.serialization.Serializable

@Serializable
data class Meta(val schemaVersion: Int, val mods: List<Mod>) {
    @Serializable
    data class Mod(
        val modid: String,
        val name: String,
        val description: String,
        val versions: List<ModVersion>,
        val recommended: Boolean,
        val traits: List<String>,
        val incompatibilities: List<String>
    ) {
        fun getModVersion(minecraftVersion: String): ModVersion? = this.versions.firstOrNull { minecraftVersion in it.target_version }
    }

    @Serializable
    data class ModVersion(val target_version: List<String>, val version: String, val url: String, val hash: String, val recommended: Boolean = true)
}
