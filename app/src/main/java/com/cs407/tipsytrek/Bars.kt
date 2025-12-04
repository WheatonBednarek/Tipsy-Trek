package com.cs407.tipsytrek

import kotlinx.serialization.Serializable

@Serializable
data class Bar(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val themeColor: Long
)

val MadisonBars = listOf(
    Bar("Wando’s", 43.0733606, -89.3959882, 0xFFFF9800),
    Bar("Chasers", 43.0740257, -89.3932565, 0xFF3F51B5),
    Bar("Whiskey Jack’s", 43.0751373, -89.3948362, 0xFFFF9800),
    Bar("Kollege Klub", 43.0756532, -89.397155, 0xFFE91E63),
    Bar("The Double U", 43.0734728, -89.3968009, 0xFFFF9800),
    Bar("State Street Brats", 43.0746718, -89.3959904, 0xFFFF9800),
    Bar("Red Rock Saloon", 43.0751186, -89.3917812, 0xFFFF9800),
    Bar("Nitty Gritty", 43.0718112, -89.3956146, 0xFFFF9800),
    Bar("Red Shed", 43.0750728, -89.3937564, 0xFFFF9800),
    Bar("Vintage Spirits & Grill", 43.0729614, -89.3954472, 0xFFFF9800),
    Bar("Mondays", 43.0730179, -89.3943058, 0xFFFF9800),
    Bar("Lucky’s 1313 Brew Pub", 43.0675011, -89.4081645, 0xFFFF9800),
    Bar("SCONNIEBAR", 43.0676284, -89.4101684, 0xFFFF9800),
    Bar("The Library Cafe & Bar", 43.0730465, -89.4092222, 0xFFFF9800)
)