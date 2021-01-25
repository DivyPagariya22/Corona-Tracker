package com.example.android.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.example.android.covidtracker.databinding.ActivityMainBinding
import com.google.gson.GsonBuilder
import com.robinhood.spark.SparkView
import com.robinhood.ticker.TickerUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL= "https://api.covidtracking.com/v1/"
private const val TAG = "MainActivity"
private const val ALL_STATES = "All (Nationwide)"

class MainActivity : AppCompatActivity() {

    private lateinit var currentShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var StateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        //Instance of Covid Service
        val covidService = retrofit.create(CovidService::class.java)

        //Fetch national data
        covidService.getNationalData().enqueue(object: Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG,"onResponce $response")
                val nationaldata = response.body()
                if(nationaldata == null){
                    Log.w(TAG,"did not recive valid responce body ")
                    return
                }
                nationalDailyData = nationaldata.reversed()
                setUpEventListner()
                Log.i(TAG,"update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {

                Log.e(TAG,"on Failure $t")
            }

        })

        //Fetch State Data
        covidService.getStateData().enqueue(object: Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG,"onResponce $response")
                val stateData = response.body()
                if(stateData == null){
                    Log.w(TAG,"did not recive valid responce body ")
                    return
                }
                StateDailyData = stateData
                    .filter { it.dateChecked != null }
                    .reversed()
                    .groupBy { it.state }
                Log.i(TAG,"update spinner with state names")
                updateSpinnerWithStateData(StateDailyData.keys)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {

                Log.e(TAG,"on Failure $t")
            }

        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0, ALL_STATES)
        binding.spinnerSelect.attachDataSource(stateAbbreviationList)
        binding.spinnerSelect.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = StateDailyData[selectedState] ?: nationalDailyData
            updateDisplayWithData(selectedData)
        }
    }

    private fun setUpEventListner() {
        //Ticker Animation
        binding.tvMetric.setCharacterLists(TickerUtils.provideNumberList())


        // Listner for user for scrubbing chart
        binding.sparkView.isScrubEnabled = true
        binding.sparkView.setScrubListener { itemData->
            if(itemData is CovidData) {
                updateInfoforDate(itemData)
            }
        }

        //respond to radioButtons
        binding.radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.timeAgo = when(checkedId){
                R.id.radioButtonWeek -> TimeScale.Week
                R.id.radioButtonMonth -> TimeScale.Month
                else -> TimeScale.Max
            }
            adapter.notifyDataSetChanged()
        }

        binding.radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.Negative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.Positive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.Death -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        //update the color
        @ColorRes val colorRes = when (metric) {
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        binding.sparkView.lineColor = colorInt
        binding.tvMetric.textColor = colorInt


        adapter.metric = metric
        adapter.notifyDataSetChanged()

        updateInfoforDate(currentShownData.last())

    }

    private fun updateDisplayWithData(DailyData: List<CovidData>) {
        currentShownData = DailyData
        // Create a new SparkAdapter with the data
         adapter = CovidSparkAdapter(DailyData)
        binding.sparkView.adapter = adapter

        // Update radio buttons to select positive cases and max time by default
        binding.radioButtonMax.isChecked = true
        updateDisplayMetric(Metric.POSITIVE)
        binding.Positive.isChecked = true

        // Display metric for most recent date
        updateInfoforDate(DailyData.last())
    }

    private fun updateInfoforDate(CovidData: CovidData) {
        val numCase = when(adapter.metric){
            Metric.POSITIVE -> CovidData.positiveIncrease
            Metric.NEGATIVE -> CovidData.negativeIncrease
            Metric.DEATH -> CovidData.deathIncrease
        }
        binding.tvMetric.text = NumberFormat.getInstance().format(numCase)
       val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        binding.tvDateLabel.text = outputDateFormat.format(CovidData.dateChecked)
    }
}