package com.pistacium.modcheck

import kotlinx.serialization.Serializable

@Serializable
class MmcPackJson(val components: Array<Component>, val formatVersion: Int) {
    @Serializable
    class Component(val uid: String, val version: String)
}
