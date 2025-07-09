import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val flights = dataFrameOf(
        "city" to columnOf("Moscow", "London", "Paris", "New York", "Tokyo"),
    )

    val cities = dataFrameOf(
        "city" to columnOf("Moscow", "London", "Paris", "New York", "Tokyo", "Berlin", "Los Angeles", "Seoul"),
        "coordinates" to columnOf(
            "55.7558" to "37.6176",    // Moscow
            "51.5074" to "-0.1278",    // London
            "48.8566" to "2.3522",     // Paris
            "40.7128" to "-74.0060",   // New York
            "35.6762" to "139.6503",   // Tokyo
            "52.5200" to "13.4050",    // Berlin
            "34.0522" to "-118.2437",  // Los Angeles
            "37.5665" to "126.9780"    // Seoul
        )
    )

    val df = flights.leftJoin(cities)
    df.assert()

    return "OK"
}
