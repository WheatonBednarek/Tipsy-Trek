package com.cs407.tipsytrek.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object Achievements {

    var localBarVisitCount = 0
    val localUnlockedAchievements = mutableSetOf<String>()


    fun recordBarVisit(onUnlocked: (String) -> Unit) {
        localBarVisitCount += 1

        checkBarVisitAchievements(localBarVisitCount, onUnlocked)
    }

    fun checkBarVisitAchievements(
        barCount: Int,
        onUnlocked: (String) -> Unit
    ) {
        val achievements = listOf(
            1 to "First Bar!",
            3 to "Bar Hopper",
            5 to "Neighborhood Legend",
            10 to "Madison Nightlife Master"
        )

        achievements.forEach { (threshold, name) ->
            if (barCount >= threshold && name !in localUnlockedAchievements) {
                localUnlockedAchievements.add(name)
                onUnlocked(name)
            }
        }
    }

    fun checkDrinkAchievements(context: Context,
                               drinkCount: Int,
                               onUnlocked: (String) -> Unit) {
        val achievements = listOf(
            2 to "Getting Started",
            5 to "Feeling Buzzed",
            10 to "Party Professional",
            20 to "Tipsy Trek Champion"
        )

        achievements.forEach { (threshold, name) ->
            if (drinkCount >= threshold) {
                unlock("", name, "Consumed $threshold drinks.", onUnlocked, context)
            }
        }
    }

    fun unlock(
        userId: String,
        name: String,
        description: String,
        onUnlocked: (String) -> Unit,
        context: Context
    ) {
        val newlyUnlocked = LocalAchievements.unlock(context, name)

        if (newlyUnlocked) {
            onUnlocked(name)
        }
    }
}

object LocalAchievements {

    private const val PREFS = "achievements"

    fun getUnlocked(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet("unlocked", emptySet()) ?: emptySet()
    }

    fun unlock(context: Context, name: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet("unlocked", emptySet())?.toMutableSet() ?: mutableSetOf()

        return if (!current.contains(name)) {
            current.add(name)
            prefs.edit().putStringSet("unlocked", current).apply()
            Log.d("Achievements", "Unlocked locally: $name")
            true
        } else {
            false
        }
    }
}