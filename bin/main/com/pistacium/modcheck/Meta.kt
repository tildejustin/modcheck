package com.pistacium.modcheck

import kotlinx.serialization.*

@Serializable
data class Meta(val schemaVersion: Int, val mods: List<Mod>) {
    @Serializable
    data class Mod @OptIn(ExperimentalSerializationApi::class) constructor(
        val modid: String,
        val name: String,
        val description: String,
        val sources: String,
        val versions: List<ModVersion>,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val traits: List<String> = listOf(),
        @EncodeDefault(EncodeDefault.Mode.NEVER) val incompatibilities: List<String> = listOf(),
        @EncodeDefault(EncodeDefault.Mode.NEVER) val recommended: Boolean = true,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val obsolete: Boolean = false
    ) {
        fun getModVersion(minecraftVersion: String): ModVersion? = this.versions.firstOrNull { minecraftVersion in it.target_version }
    }

    @Serializable
    data class ModVersion @OptIn(ExperimentalSerializationApi::class) constructor(
        val target_version: List<String>,
        val version: String,
        val url: String,
        val hash: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val recommended: Boolean = true,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val obsolete: Boolean = false
    )
}
