package com.cs407.tipsytrek

import kotlinx.serialization.Serializable

@Serializable
data class Bar(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

val MadisonBars = listOf(
    Bar("Wando’s", 43.0733606, -89.3959882),
    Bar("Chasers", 43.0740257, -89.3932565),
    Bar("Whiskey Jack’s", 43.0751373, -89.3948362),
    Bar("Kollege Klub", 43.0756532, -89.397155),
    Bar("The Double U", 43.0734728, -89.3968009),
    Bar("State Street Brats", 43.0746718, -89.3959904),
    Bar("Red Rock Saloon", 43.0751186, -89.3917812),
    Bar("Nitty Gritty", 43.0718112, -89.3956146),
    Bar("Red Shed", 43.0750728, -89.3937564),
    Bar("Vintage Spirits & Grill", 43.0729614, -89.3954472),
    Bar("Mondays", 43.0730179, -89.3943058),
    Bar("Lucky’s 1313 Brew Pub", 43.0675011, -89.4081645),
    Bar("SCONNIEBAR", 43.0676284, -89.4101684),
    Bar("The Library Cafe & Bar", 43.0730465, -89.4092222)
)