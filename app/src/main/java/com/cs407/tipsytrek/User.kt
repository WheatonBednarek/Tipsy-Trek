package com.cs407.tipsytrek

data class User(
    val uid: String? = null,
    val email: String? = null,
    val displayName: String = "Placeholder Name",
    val username: String = "@placeholder",
    val currentDrinks: List<Beverage> = emptyList(),
    val allTimeDrinks: List<Beverage> = emptyList()
) {
    // Simple BAC model: 0.02% per standard drink
    val bac: Double
        get() = currentDrinks.sumOf { it.standardDrinks } * 0.02

    val formattedBac: String
        get() = String.format("%.2f%%", bac)

    fun addDrink(drink: Beverage): User =
        copy(
            currentDrinks = currentDrinks + drink,
            allTimeDrinks = allTimeDrinks + drink
        )

    fun resetCurrentDrinks(): User =
        copy(currentDrinks = emptyList())
}
