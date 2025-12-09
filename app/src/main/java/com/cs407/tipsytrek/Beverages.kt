package com.cs407.tipsytrek

import kotlinx.serialization.Serializable

@Serializable
data class Beverage(
    val name: String = "",
    val color: Long = 0L,
    val drinkType: Int = 1,
    val standardDrinks: Double = 0.0
)

