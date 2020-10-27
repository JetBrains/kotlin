/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.example.weather_func

import platform.posix.*
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.refTo
import kotlin.system.exitProcess

private val API_KEY by lazy { fetchApiKey() }

fun main(args: Array<String>) {
	val input = readLine()
	val location = fetchLocationArg(input)
	val jsonFile = fetchJsonFileArg(input)

	if (location.isNotEmpty()) {
		printFromWeatherService(location)
	} else if (jsonFile.isNotEmpty()) {
		checkFile(jsonFile)
		printJsonFile(jsonFile)
	} else {
		handleEmptyInput()
	}
	println("Exiting...")
}

private fun handleEmptyInput() {
	println(
		"""
		| Weather - A OpenFaaS Serverless Function that outputs weather information.
		| Usage examples (pass one of the following as input to the function):
		|   * Location (uses the Open Weather Map service), eg:
		|       -l="christchurch,nz"
		|   * JSON File, eg: -f="weather.json"
		""".trimMargin()
	)
	exitProcess(0)
}

private fun checkFile(file: String) {
	val error = -1
	if (access(file, F_OK) == error) {
		println("File $file doesn't exist!")
		exitProcess(error)
	}
}

private fun fetchJson(jsonFile: String): String {
	var result = ""
	// Open the file using the fopen function and store the file handle.
	val file = fopen(jsonFile, "r")
	fseek(file, 0, SEEK_END)
	val fileSize = ftell(file)
	fseek(file, 0, SEEK_SET)

	memScoped {
		val buffer = allocArray<ByteVar>(fileSize)
		// Read the entire file and store the contents into the buffer.
		fread(buffer, fileSize, 1, file)
		result = buffer.toKString()
	}
	// Close the file.
	fclose(file)
	return result
}

private fun printJsonFile(jsonFile: String) {
	println("Printing from JSON file ($jsonFile)...")
	val weather = createWeatherFromJson(fetchJson(jsonFile))
	println("Weather Object:\n$weather")
	println("Weather JSON:\n${weatherToJsonString(weather)}")
}

private fun printFromWeatherService(location: String) {
	println("Fetching weather information (for $location)...")
	val curl = CUrl(createUrl(location)).apply {
		header += { if(it.startsWith("HTTP")) println("Response Status: $it") }
		body += { data ->
			val weather = createWeatherFromJson(data)
			println("Weather information:\n${weatherToJsonString(weather)}")
		}
	}
	curl.fetch()
	curl.close()
}

private fun fetchJsonFileArg(input: String?): String {
	val flag = "-f"
	return if (input != null && input.startsWith("$flag=")) input.replace("$flag=", "").replace("\"", "")
	else ""
}

private fun fetchLocationArg(input: String?): String {
	val flag = "-l"
	return if (input != null && input.startsWith("$flag=")) input.replace("$flag=", "").replace("\"", "")
	else ""
}

private fun createUrl(location: String): String {
	val baseUrl = "http://api.openweathermap.org/data/2.5/weather"
	return "$baseUrl?q=$location&units=metric&appid=$API_KEY"
}

private fun fetchApiKey(): String {
	var result = ""
	val maxChars = 50
    // utfCharSize in bytes.
    val utfCharSize = 4
	// Open the file using the fopen function and store the file handle.
	val file = fopen("openweathermap_key.txt", "r")
	val buffer = ByteArray(utfCharSize * maxChars)
	if (file != null) result = fgets(buffer.refTo(0), buffer.size, file)?.toKString()?.trim() ?: ""
	// Close the file.
	fclose(file)
	return result
}
