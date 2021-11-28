/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.example.weather_func

import cjson.cJSON_CreateObject as createJsonObject
import cjson.cJSON_AddStringToObject as addStringToObject
import cjson.cJSON_AddNumberToObject as addNumberToObject
import cjson.cJSON_AddArrayToObject as addArrayToObject
import cjson.cJSON_AddItemToArray as addItemToArray
import cjson.cJSON_AddItemToObjectCS as addItemToObject

import cjson.cJSON_Delete as deleteObject
import cjson.cJSON_Print as jsonString
import cjson.cJSON_Parse as parseJson
import cjson.cJSON_GetObjectItemCaseSensitive as jsonObjectItem
import cjson.cJSON_IsString as jsonValueIsString

import cjson.cJSON_IsNumber as jsonValueIsNumber
import cjson.cJSON_GetErrorPtr as getErrorPointer
import cjson.cJSON_GetStringValue as jsonStringValue
import cjson.cJSON_GetArraySize as jsonArraySize
import cjson.cJSON_GetArrayItem as jsonArrayItem
import cjson.cJSON

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.toKString
import kotlinx.cinterop.pointed
import kotlin.system.exitProcess

private val trueValue = 1

internal fun createWeatherFromJson(jsonStr: String): Weather {
	val rootObj = parseJson(jsonStr)
	checkForError()
	val result = createWeather(
		placeName = jsonObjectItem(rootObj, "name").stringValue(),
		countryCode = extractCountryCode(rootObj),
		windInfo = extractWindInfo(rootObj),
		conditions = extractConditions(rootObj),
		tempInfo = extractTempInfo(rootObj),
		humidity = extractHumidity(rootObj)
	)
	if (rootObj != null) deleteObject(rootObj)
	return result
}

/**
 * Creates a new Weather object.
 * @param placeName Name of the place.
 * @param countryCode Unique country code.
 * @param windInfo A Pair with wind speed as 1st item, and wind degrees as 2nd item.
 * @param conditions A Array of Pair. Each Pair has name as 1st item, and desc as 2nd item.
 * @param humidity General humidity of the atmosphere.
 * @param tempInfo A Triple with temperature as 1st item, min temperature as 2nd item, and max temperature as 3rd item.
 * @return A instance of Weather.
*/
private fun createWeather(
	placeName: String, 
	countryCode: String, 
	windInfo: Pair<Double, Int>,
	conditions: Array<Pair<String, String>>,
	humidity: Int,
	tempInfo: Triple<Int, Int, Int>
) = Weather(
	placeName = placeName,
	countryCode = countryCode,
	conditions = conditions,
	humidity = humidity,
	windSpeed = windInfo.first,
	windDegrees = windInfo.second,
	temp = tempInfo.first,
	minTemp = tempInfo.second,
	maxTemp = tempInfo.third
)

private fun extractTempInfo(rootObj: CPointer<cJSON>?): Triple<Int, Int, Int> {
	val mainObj = jsonObjectItem(rootObj, "main")
	val temp = jsonObjectItem(mainObj, "temp").intValue()
	val minTemp = jsonObjectItem(mainObj, "temp_min").intValue()
	val maxTemp = jsonObjectItem(mainObj, "temp_max").intValue()

	return Triple(temp, minTemp, maxTemp)
}

private fun extractHumidity(rootObj: CPointer<cJSON>?): Int {
	val mainObj = jsonObjectItem(rootObj, "main")

	return jsonObjectItem(mainObj, "humidity").intValue()
}

private fun extractConditions(rootObj: CPointer<cJSON>?): Array<Pair<String, String>> {
	val tmp = mutableListOf<Pair<String, String>>()
	val weatherObj = jsonObjectItem(rootObj, "weather")

	for (pos in 0..(jsonArraySize(weatherObj) - 1)) {
		val item = jsonArrayItem(weatherObj, pos)
		val name = jsonObjectItem(item, "main").stringValue()
		val desc = jsonObjectItem(item, "description").stringValue()

		tmp += (name to desc)
	}
	return tmp.toTypedArray()
}

private fun extractWindInfo(rootObj: CPointer<cJSON>?): Pair<Double, Int> {
	val windObj = jsonObjectItem(rootObj, "wind")
	val windSpeed = jsonObjectItem(windObj, "speed").doubleValue()
	val windDegrees = jsonObjectItem(windObj, "deg").intValue()

	return (windSpeed to windDegrees)
}

private fun extractCountryCode(rootObj: CPointer<cJSON>?): String {
	val sys = jsonObjectItem(rootObj, "sys")
	
	return jsonObjectItem(sys, "country").stringValue()
}

internal fun weatherToJsonString(weather: Weather) : String {
	val rootObj = createJsonObject()
	addStringToObject(rootObj, "placeName", weather.placeName)
	addStringToObject(rootObj, "countryCode", weather.countryCode)
	addNumberToObject(rootObj, "humidity", weather.humidity.toDouble())
	addTempInfoArray(rootObj, temp = weather.temp, minTemp = weather.minTemp, maxTemp = weather.maxTemp)
	addConditionsArray(rootObj, weather.conditions)
	val result = jsonString(rootObj)?.toKString() ?: ""
	deleteObject(rootObj)
	return result
}

private fun addTempInfoArray(rootObj: CPointer<cJSON>?, temp: Int, minTemp: Int, maxTemp: Int) {
	val tmpObj = createJsonObject()

	addNumberToObject(tmpObj, "temp", temp.toDouble())
	addNumberToObject(tmpObj, "minTemp", minTemp.toDouble())
	addNumberToObject(tmpObj, "maxTemp", maxTemp.toDouble())
	addItemToObject(rootObj, "tempInfo", tmpObj)
}

private fun addConditionsArray(rootObj: CPointer<cJSON>?, conditions: Array<Pair<String, String>>) {
	val tmpArr = addArrayToObject(rootObj, "conditions")

	conditions.forEach { (name, desc) ->
		val tmpObj = createJsonObject()
		addStringToObject(tmpObj, "name", name)
		addStringToObject(tmpObj, "desc", desc)
		addItemToArray(tmpArr, tmpObj)
	}
}

private fun checkForError() {
	val errorMsg = getErrorPointer()?.toKString() ?: ""

	if (errorMsg.isNotEmpty()) {
    	println("JSON error before: $errorMsg\n")
    	exitProcess(-1)
    }
}

private fun CPointer<cJSON>?.intValue() = this?.pointed?.valueint ?: 0

private fun CPointer<cJSON>?.doubleValue() = this?.pointed?.valuedouble ?: 0.0

private fun CPointer<cJSON>?.stringValue() = jsonStringValue(this)?.toKString() ?: ""
