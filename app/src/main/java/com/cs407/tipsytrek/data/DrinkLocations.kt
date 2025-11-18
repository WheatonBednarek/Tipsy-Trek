package com.cs407.tipsytrek.data

import com.cs407.tipsytrek.Beverage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

data class LocatedDrink(val beverage: Beverage, val lat: Double, val long: Double, val expiryTime: Long)
class DrinkManager {

    companion object {
        // each longitude has 60 minutes and each minute has 60 seconds;
        // we want 30 seconds (≈ regent to spooner street distance)
        const val defaultRadius: Double = (1.0 / 60.0 / 60.0) * 30
        const val numDrinksAdd = 3
        const val newDrinkIntervalSeconds = 60
        const val drinkExpiryTimeSeconds = 60 * 5
        val possibleBevs = listOf(
            Beverage("Coors Banquet", 0xfff8e28c, 20 * 0.05),
            Beverage("Spotted Cow", 0xfff6e07a, 20 * 0.048),
            Beverage("Guinness", 0xff1a0f07, 20 * 0.042),
            Beverage("Heineken", 0xfff7e88a, 20 * 0.05),
            Beverage("Blue Moon", 0xffe9c66f, 20 * 0.054),
            Beverage("Budweiser", 0xfff7e07a, 20 * 0.05),
            Beverage("Bud Light", 0xfff8eb9a, 20 * 0.042),
            Beverage("Modelo Especial", 0xfff6e076, 20 * 0.044),
            Beverage("Corona Extra", 0xfff7e995, 20 * 0.046),
            Beverage("Stella Artois", 0xfff6e281, 20 * 0.052),
            Beverage("Pabst Blue Ribbon", 0xfff7e38e, 20 * 0.047),
            Beverage("Miller Lite", 0xfff7ec9b, 20 * 0.042),
            Beverage("Sam Adams Boston Lager", 0xffc77027, 20 * 0.05),
            Beverage("Lagunitas IPA", 0xffd99a3d, 20 * 0.065),
            Beverage("Sierra Nevada Pale Ale", 0xffd5a64a, 20 * 0.055),
            Beverage("Newcastle Brown Ale", 0xff5f2f12, 20 * 0.05),
            Beverage("Fat Tire Amber Ale", 0xffc06724, 20 * 0.055),
            Beverage("Dos Equis Lager", 0xfff6df72, 20 * 0.047),
            Beverage("Shiner Bock", 0xff4b1e0e, 20 * 0.048),
            Beverage("Yuengling Lager", 0xff853e12, 20 * 0.045),
            Beverage("New Glarus Raspberry Tart", 0xffb0253c, 20 * 0.04),
            Beverage("New Glarus Moon Man", 0xfff3d27c, 20 * 0.05),
            Beverage("Leinenkugel's Original", 0xfff7e07a, 20 * 0.045),
            Beverage("Leinenkugel's Honey Weiss", 0xfff4d77a, 20 * 0.055),
            Beverage("Toppling Goliath Pseudo Sue", 0xfff2c55c, 20 * 0.065),
            Beverage("Bell's Two Hearted Ale", 0xffd9953d, 20 * 0.07),
            Beverage("Bell's Oberon", 0xffe5b44f, 20 * 0.057),
            Beverage("Founders All Day IPA", 0xffddb14a, 20 * 0.042),
            Beverage("Goose Island 312 Urban Wheat", 0xfff1d67b, 20 * 0.042),
            Beverage("Surly Furious", 0xffb1441d, 20 * 0.067),
            Beverage("White Claw Black Cherry", 0x66ffffff, 20 * 0.05),
            Beverage("Truly Wild Berry", 0x66ffffff, 20 * 0.05),
            Beverage("La Marca Prosecco", 0xfff7eaa4, 20 * 0.11),
            Beverage("Josh Cabernet Sauvignon", 0xff3c0a0f, 20 * 0.13),
            Beverage("Apothic Red Blend", 0xff4b0d12, 20 * 0.13),
            Beverage("Jameson Irish Whiskey", 0xffc58527, 20 * 0.40),
            Beverage("Patrón Silver Tequila", 0x66ffffff, 20 * 0.40),
            Beverage("Captain Morgan Spiced Rum", 0xffb56a1d, 20 * 0.35),
            Beverage("Aperol Spritz", 0xfff98a2d, 20 * 0.11),
            Beverage("Margarita", 0xccd6e88a, 20 * 0.13)
        )
    }

    private val savedDrinks: MutableList<LocatedDrink> = mutableListOf()
    private val _drinksFlow = MutableStateFlow<List<LocatedDrink>>(emptyList())
    val drinksFlow: StateFlow<List<LocatedDrink>> get() = _drinksFlow

    private var prevAdd: Long = 0

    fun tick(lat: Double, long: Double, radius: Double = defaultRadius) {
        val now = System.currentTimeMillis()

        // remove expired drinks
        val itr = savedDrinks.iterator()
        while (itr.hasNext()) {
            val drink = itr.next()
            if (drink.expiryTime < now) {
                itr.remove()
            }
        }

        if (now - prevAdd > 1000 * newDrinkIntervalSeconds) {
            prevAdd = now
            repeat(numDrinksAdd) {
                savedDrinks.add(
                    newBeverage(
                        lat + Random.nextDouble() * radius * 2,
                        long + Random.nextDouble() * radius * 2
                    )
                )
            }
        }

        _drinksFlow.value = savedDrinks.toList()
    }

    private fun newBeverage(lat: Double, long: Double): LocatedDrink {
        return LocatedDrink(
            possibleBevs.random(),
            lat,
            long,
            System.currentTimeMillis() + drinkExpiryTimeSeconds * 1000
        )
    }
}