/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.stats

import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.util.UnitStats
import java.io.File
import java.util.SortedSet
import kotlin.collections.set

object FileUtils {
    fun extractJsonReportFiles(fileOrDirectoryPath: String): Array<File>? {
        val fileOrDirectory = File(fileOrDirectoryPath)
        if (!fileOrDirectory.exists()) {
            println("❗The file or directory ${fileOrDirectory.absolutePath} does not exist")
            return null
        }

        return if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles { it.extension == "json" }.takeIf { it?.isNotEmpty() == true } ?: run {
                println("❗The directory ${fileOrDirectory.absolutePath} does not contain json dumps")
                return null
            }
        } else {
            arrayOf(fileOrDirectory.takeIf { it.exists() && it.extension == "json" } ?: run {
                println("❗The file ${fileOrDirectory.absolutePath} doesn't have json extension")
                return null
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun deserializeJsonReports(jsonFiles: Array<File>, analyzedModuleName: String?): ReportsData {
        val gsonBuilder = GsonBuilder().create()

        val reportsData = if (analyzedModuleName != null) {
            sortedSetOf<UnitStats>()
        } else {
            mutableMapOf<String, UnitStats>()
        }

        var reportsIgnoredWarningReported = false

        for (jsonFile in jsonFiles) {
            try {
                fun reportConflictingTimeStampsWarning() {
                    if (!reportsIgnoredWarningReported) {
                        println("⚠ The dump directory contains reports with conflicting timestamps (probably the property `${UnitStats::timeStampMs.name}` is not set or set incorrectly)")
                        println("To get rid of the warning, please perform a fresh build with `-Xdump-perf` to an empty directory")
                        reportsIgnoredWarningReported = true
                    }
                }

                val moduleStats = gsonBuilder.fromJson(jsonFile.readText(), UnitStats::class.java)
                val name = moduleStats.name
                if (name != null) {
                    if (name == analyzedModuleName) {
                        val timeStampReports = reportsData as SortedSet<UnitStats>
                        if (!timeStampReports.add(moduleStats)) {
                            reportConflictingTimeStampsWarning()
                        }
                    } else {
                        val moduleReports = reportsData as MutableMap<String, UnitStats>
                        val existingModuleStats = moduleReports[name]
                        if (existingModuleStats != null) {
                            if (!reportsIgnoredWarningReported) {
                                // Report only the single warning for the entire directory
                                // to avoid reporting of many multiple messages if a user runs the utility
                                // on a directory that contains info about multiple builds.
                                println("⚠ The dump directory contains reports that are outdated and ignored (probably they are results of a previous build)")
                                println("To get rid of the warning, please remove outdated reports or perform fresh build with `-Xdump-perf` to an empty directory")
                                reportsIgnoredWarningReported = true
                            }
                        }
                        if (existingModuleStats == null || moduleStats.timeStampMs > existingModuleStats.timeStampMs) {
                            // Replace the older module with the new one in case of name collision.
                            moduleReports[name] = moduleStats
                        } else if (moduleStats.timeStampMs == existingModuleStats.timeStampMs) {
                            reportConflictingTimeStampsWarning()
                        }
                    }
                } else {
                    println("⚠ The file ${jsonFile.absolutePath} is ignored because it doesn't contain `name` property")
                }
            } catch (ex: Exception) {
                println("❗An error occurred while reading or deserializing ${jsonFile.absolutePath}: ${ex.message}")
            }
        }

        return if (analyzedModuleName != null) {
            TimestampReportsData(analyzedModuleName, reportsData as SortedSet<UnitStats>)
        } else {
            ModulesReportsData(reportsData as Map<String, UnitStats>)
        }
    }
}