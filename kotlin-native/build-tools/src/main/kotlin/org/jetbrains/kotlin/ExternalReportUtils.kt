package org.jetbrains.kotlin


import com.google.gson.annotations.*
import com.google.gson.*
import com.google.gson.stream.JsonReader
import java.io.File
import java.io.FileReader
import java.io.PrintWriter


data class ExternalTestReport(@Expose val statistics: Statistics, @Expose val groups: List<KonanTestGroupReport>)

fun saveReport(reportFileName: String, statistics: Statistics){
    File(reportFileName).apply {
        parentFile.mkdirs()
        PrintWriter(this).use {
            it.append(gson.toJson(ExternalTestReport(statistics, emptyList())))
        }
    }
}

internal val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()!!
fun loadReport(reportFileName: String) : ExternalTestReport = JsonReader(FileReader(reportFileName)).use {
    gson.fromJson(it, ExternalTestReport::class.java)
}
