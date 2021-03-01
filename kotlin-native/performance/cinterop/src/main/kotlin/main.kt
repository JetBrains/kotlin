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


import org.jetbrains.structsProducedByMacrosBenchmarks.*
import org.jetbrains.benchmarksLauncher.*
import org.jetbrains.structsBenchmarks.*
import org.jetbrains.typesBenchmarks.*
import kotlinx.cli.*

class CinteropLauncher : Launcher() {
    override val benchmarks = BenchmarksCollection(
            mutableMapOf(
                    "macros" to BenchmarkEntry(::macrosBenchmark),
                    "struct" to BenchmarkEntry(::structBenchmark),
                    "union" to BenchmarkEntry(::unionBenchmark),
                    "enum" to BenchmarkEntry(::enumBenchmark),
                    "stringToC" to BenchmarkEntryWithInit.create(::StringBenchmark, { stringToCBenchmark() }),
                    "stringToKotlin" to BenchmarkEntryWithInit.create(::StringBenchmark, { stringToKotlinBenchmark() }),
                    "intMatrix" to BenchmarkEntryWithInit.create(::IntMatrixBenchmark, { intMatrixBenchmark() }),
                    "int" to BenchmarkEntryWithInit.create(::IntBenchmark, { intBenchmark() }),
                    "boxedInt" to BenchmarkEntryWithInit.create(::BoxedIntBenchmark, { boxedIntBenchmark() })
            )
    )
}

fun main(args: Array<String>) {
    val launcher = CinteropLauncher()
    BenchmarksRunner.runBenchmarks(args, { arguments: BenchmarkArguments ->
        if (arguments is BaseBenchmarkArguments) {
            launcher.launch(arguments.warmup, arguments.repeat, arguments.prefix,
                    arguments.filter, arguments.filterRegex, arguments.verbose)
        } else emptyList()
    }, benchmarksListAction = launcher::benchmarksListAction)
}