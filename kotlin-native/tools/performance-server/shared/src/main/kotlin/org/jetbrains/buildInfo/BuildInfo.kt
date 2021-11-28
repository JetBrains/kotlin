/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.buildInfo

import org.jetbrains.report.*
import org.jetbrains.report.json.*

data class Build(val buildNumber: String, val startTime: String, val finishTime: String, val branch: String,
                 val commits: String, val failuresNumber: Int) {

    companion object : EntityFromJsonFactory<Build> {
        override fun create(data: JsonElement): Build {
            if (data is JsonObject) {
                val buildNumber = elementToString(data.getRequiredField("buildNumber"), "buildNumber").replace("\"", "")
                val startTime = elementToString(data.getRequiredField("startTime"), "startTime").replace("\"", "")
                val finishTime = elementToString(data.getRequiredField("finishTime"), "finishTime").replace("\"", "")
                val branch = elementToString(data.getRequiredField("branch"), "branch").replace("\"", "")
                val commits = elementToString(data.getRequiredField("commits"), "commits")
                val failuresNumber = elementToInt(data.getRequiredField("failuresNumber"), "failuresNumber")
                return Build(buildNumber, startTime, finishTime, branch, commits, failuresNumber)
            } else {
                error("Top level entity is expected to be an object. Please, check origin files.")
            }
        }
    }

    private fun formatTime(time: String, targetZone: Int = 3): String {
        val matchResult = "^\\d{8}T(\\d{2})(\\d{2})\\d{2}((\\+|-)\\d{2})".toRegex().find(time)?.groupValues
        matchResult?.let {
            val timeZone = matchResult[3].toInt()
            val timeDifference = targetZone - timeZone
            var hours = (matchResult[1].toInt() + timeDifference)
            if (hours > 23) {
                hours -= 24
            }
            return "${if (hours < 10) "0$hours" else "$hours"}:${matchResult[2]}"
        } ?: error { "Wrong format of time $startTime" }
    }

    val date: String by lazy {
        val matchResult = "^(\\d{4})(\\d{2})(\\d{2})".toRegex().find(startTime)?.groupValues
        matchResult?.let { "${matchResult[3]}/${matchResult[2]}/${matchResult[1]}" }
                ?: error { "Wrong format of time $startTime" }
    }
    val formattedStartTime: String by lazy {
        formatTime(startTime)
    }
    val formattedFinishTime: String by lazy {
        formatTime(finishTime)
    }
}