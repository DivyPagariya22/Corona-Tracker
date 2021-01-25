package com.example.android.covidtracker

enum class Metric{
    POSITIVE, NEGATIVE, DEATH
}

enum class TimeScale (val numDays : Int){
    Week(7),
    Month(30),
    Max(-1)
}