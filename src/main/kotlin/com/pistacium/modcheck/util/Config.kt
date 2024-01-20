package com.pistacium.modcheck.util

import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class Config(val filepath: String) {
    fun getDirectory(): Path = Path.of(filepath)
}
