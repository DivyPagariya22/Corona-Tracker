package com.example.android.covidtracker


import android.graphics.RectF
import com.robinhood.spark.SparkAdapter


class CovidSparkAdapter(private val dailyData: List<CovidData>) : SparkAdapter() {
    var  metric = Metric.POSITIVE
    var timeAgo = TimeScale.Max

    override fun getCount() = dailyData.size

    override fun getItem(index: Int) = dailyData[index]

    override fun getY(index: Int): Float {
        val chosenData = dailyData[index]
        return when(metric){
            Metric.POSITIVE -> chosenData.positiveIncrease.toFloat()
            Metric.NEGATIVE -> chosenData.negativeIncrease.toFloat()
            Metric.DEATH -> chosenData.deathIncrease.toFloat()
        }
    }

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (timeAgo != TimeScale.Max) {
            bounds.left = count - timeAgo.numDays.toFloat()
        }
        return bounds
    }

}
