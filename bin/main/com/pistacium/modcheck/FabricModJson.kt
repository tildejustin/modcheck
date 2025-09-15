package com.pistacium.modcheck

import kotlinx.serialization.Serializable

@Serializable
class FabricModJson(val version: String, var id: String, var name: String)
