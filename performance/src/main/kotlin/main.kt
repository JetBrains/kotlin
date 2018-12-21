/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.ring.Launcher
import org.jetbrains.ring.JsonReportCreator

fun main(args: Array<String>) {
    var numWarmIterations = 0       // Should be 100000 for jdk based run
    var numberOfAttempts = 10
    var jsonReport: String? = null

    when (args.size) {
        0 -> { }
        1 -> numWarmIterations = args[0].toInt()
        2 -> {
            numWarmIterations = args[0].toInt()
            numberOfAttempts = args[1].toInt()
        }
        3 -> {
            numWarmIterations = args[0].toInt()
            numberOfAttempts = args[1].toInt()
            jsonReport = args[2].toString()
        }
        else -> {
            println("Usage: perf [# warmup iterations] [# attempts] [# path of json report]")
            return
        }
    }

    println("Ring starting")
    println("  warmup  iterations count: $numWarmIterations")
    val results = Launcher(numWarmIterations, numberOfAttempts).runBenchmarks()
    if (jsonReport != null)
        JsonReportCreator(results).printJsonReport(jsonReport)
}