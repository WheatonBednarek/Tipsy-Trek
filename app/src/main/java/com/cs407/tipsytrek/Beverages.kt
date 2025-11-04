package com.cs407.tipsytrek

import kotlinx.serialization.Serializable

@Serializable
data class Beverage(
    val name: String,
    val color: Long
)
