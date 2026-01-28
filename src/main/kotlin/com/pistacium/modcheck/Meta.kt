package com.pistacium.modcheck

import kotlinx.serialization.*

@Serializable
data class Meta(val schemaVersion: Int, val mods: List<Mod>) {
    @Serializable
    @OptIn(ExperimentalSerializationApi::class)
    data class Mod(
        val modid: String,
        val name: String,
        val description: String,
        val versions: List<ModVersion>,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val traits: List<String> = listOf(),
        @EncodeDefault(EncodeDefault.Mode.NEVER) val incompatibilities: List<String> = listOf(),
        @EncodeDefault(EncodeDefault.Mode.NEVER) val recommended: Boolean = true,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val obsolete: Boolean = false
    ) {
        fun getModVersion(minecraftVersion: String): ModVersion? = this.versions.firstOrNull { minecraftVersion in it.target_version }
    }

    @Serializable
    @OptIn(ExperimentalSerializationApi::class)
    data class ModVersion(
        val target_version: List<String>,
        val version: String,
        val url: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val recommended: Boolean = true,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val obsolete: Boolean = false
    )
}
