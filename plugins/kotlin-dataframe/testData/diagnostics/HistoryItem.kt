package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.take
import org.jetbrains.kotlinx.dataframe.api.select
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.io.readJson

@DataSchema
interface HistoryItem {
    val header: String
    val title: String
    val titleUrl: String?
    val subtitles: DataFrame<Subtitle>
    val time: String
    val products: List<String>
    val activityControls: List<String>
    val details: DataFrame<Details>
}

interface Subtitle {
    val name: String
    val url: String?
}
interface Details {
    val name: String
}

fun main() {
    val df = DataFrame.readJson("").cast<HistoryItem>()

    df.select { time }.add("id") { index() }.take(20).<!SYNTAX!><!>
}

fun box() = "OK"
