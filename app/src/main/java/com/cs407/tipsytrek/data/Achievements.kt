// Achievements.kt
package com.cs407.tipsytrek.data

// No Android, no Firebase imports needed now

object Achievements {

    data class Achievement(
        val id: String,
        val name: String,
        val description: String,
        val threshold: Int
    )

    // 🔹 Bar-visit based achievements
    private val barVisitAchievements = listOf(
        Achievement(
            id = "bar_1",
            name = "First Bar!",
            description = "Visit your first bar.",
            threshold = 1
        ),
        Achievement(
            id = "bar_3",
            name = "Bar Hopper",
            description = "Visit 3 different bars.",
            threshold = 3
        ),
        Achievement(
            id = "bar_5",
            name = "Neighborhood Legend",
            description = "Visit 5 different bars.",
            threshold = 5
        ),
        Achievement(
            id = "bar_10",
            name = "Madison Nightlife Master",
            description = "Visit 10 bars.",
            threshold = 10
        )
    )

    // 🔹 Drink-count based achievements
    private val drinkAchievements = listOf(
        Achievement(
            id = "drink_2",
            name = "Getting Started",
            description = "Consumed 2 drinks.",
            threshold = 2
        ),
        Achievement(
            id = "drink_5",
            name = "Feeling Buzzed",
            description = "Consumed 5 drinks.",
            threshold = 5
        ),
        Achievement(
            id = "drink_10",
            name = "Party Professional",
            description = "Consumed 10 drinks.",
            threshold = 10
        ),
        Achievement(
            id = "drink_20",
            name = "Tipsy Trek Champion",
            description = "Consumed 20 drinks.",
            threshold = 20
        )
    )

    // 🔹 Which bar achievements are unlocked for given barCount
    fun unlockedBarAchievements(barCount: Int): List<Achievement> =
        barVisitAchievements.filter { barCount >= it.threshold }

    // 🔹 Which drink achievements are unlocked for given drinkCount
    fun unlockedDrinkAchievements(drinkCount: Int): List<Achievement> =
        drinkAchievements.filter { drinkCount >= it.threshold }

    // 🔹 Convenience: just the names, for the profile UI
    fun unlockedAchievementNames(barCount: Int, drinkCount: Int): List<String> =
        (unlockedBarAchievements(barCount) + unlockedDrinkAchievements(drinkCount))
            .map { it.name }
            .distinct()
}
