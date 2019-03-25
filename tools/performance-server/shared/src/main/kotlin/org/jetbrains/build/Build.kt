/*
 * Copyright 2010-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.jetbrains.build

import org.jetbrains.report.*
import org.jetbrains.report.json.*

data class Build(val buildNumber: String, val startTime: String, val finishTime: String, val branch: String,
                 val commits: String, val buildType: String, val failuresNumber: Int, val executionTime: String,
                 val compileTime: String, val codeSize: String, val bundleSize: String?) {

    companion object: EntityFromJsonFactory<Build> {
        override fun create(data: JsonElement): Build {
            if (data is JsonObject) {
                val buildNumber = elementToString(data.getRequiredField("buildNumber"), "buildNumber")
                val startTime = elementToString(data.getRequiredField("startTime"), "startTime")
                val finishTime = elementToString(data.getRequiredField("finishTime"), "finishTime")
                val branch = elementToString(data.getRequiredField("branch"), "branch")
                val commits = elementToString(data.getRequiredField("commits"), "commits")
                val buildType = elementToString(data.getRequiredField("buildType"), "buildType")
                val failuresNumber = elementToInt(data.getRequiredField("failuresNumber"), "failuresNumber")
                val executionTime = elementToString(data.getRequiredField("executionTime"), "executionTime")
                val compileTime = elementToString(data.getRequiredField("compileTime"), "compileTime")
                val codeSize = elementToString(data.getRequiredField("codeSize"), "codeSize")
                val bundleSize = elementToStringOrNull(data.getRequiredField("bundleSize"), "bundleSize")
                return Build(buildNumber, startTime, finishTime, branch, commits, buildType, failuresNumber, executionTime,
                        compileTime, codeSize, bundleSize)
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