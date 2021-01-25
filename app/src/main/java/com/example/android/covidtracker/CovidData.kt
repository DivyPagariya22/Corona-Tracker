package com.example.android.covidtracker

import java.util.*


data class CovidData(
    val dateChecked: Date,
    val deathIncrease: Int,
    val negativeIncrease: Int,
    val positiveIncrease: Int,
    val state: String
)

