package com.pistacium.modcheck.util

import kotlinx.serialization.Serializable
import java.nio.file.*

@Serializable
data class Config(val filepath: String) {
    fun getDirectory(): Path = Paths.get(filepath)
}
