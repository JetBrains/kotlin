/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("MPPTools")

package org.jetbrains.kotlin

import org.jetbrains.report.*
import org.jetbrains.report.json.*
import java.io.File

/*
 * This file includes short-cuts that may potentially be implemented in Kotlin MPP Gradle plugin in the future.
 */

fun getNativeProgramExtension(): String = when {
    PlatformInfo.isMac() -> ".kexe"
    PlatformInfo.isLinux() -> ".kexe"
    PlatformInfo.isWindows() -> ".exe"
    else -> error("Unknown host")
}

fun mergeReports(reports: List<File>): String {
    val reportsToMerge = reports.filter { it.exists() }.map {
        val json = it.inputStream().bufferedReader().use { it.readText() }
        val reportElement = JsonTreeParser.parse(json)
        BenchmarksReport.create(reportElement)
    }
    val structuredReports = mutableMapOf<String, MutableList<BenchmarksReport>>()
    reportsToMerge.map { it.compiler.backend.flags.joinToString() to it }.forEach {
        structuredReports.getOrPut(it.first) { mutableListOf<BenchmarksReport>() }.add(it.second)
    }
    val jsons = structuredReports.map { (_, value) -> value.reduce { result, it -> result + it }.toJson() }
    return when(jsons.size) {
        0 -> ""
        1 -> jsons[0]
        else -> jsons.joinToString(prefix = "[", postfix = "]")
    }
}
