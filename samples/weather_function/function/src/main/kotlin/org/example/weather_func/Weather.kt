package org.example.weather_func

/** Models the current weather for a location. */
internal data class Weather(
	val placeName: String,
	val countryCode: String,
	val windSpeed: Double = 0.0,
	val windDegrees: Int = 0,
	val temp: Int,
	val minTemp: Int,
	val maxTemp: Int,
	val humidity: Int,
	val conditions: Array<Pair<String, String>>
)
