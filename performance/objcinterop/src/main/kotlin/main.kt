/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import org.jetbrains.benchmarksLauncher.*
import org.jetbrains.complexNumbers.*
import org.jetbrains.kliopt.*

class ObjCInteropLauncher(numWarmIterations: Int, numberOfAttempts: Int, prefix: String): Launcher(numWarmIterations, numberOfAttempts, prefix) {
    val complexNumbersBecnhmark = ComplexNumbersBenchmark()
    override val benchmarks = BenchmarksCollection(
            mutableMapOf(
                    "generateNumbersSequence" to complexNumbersBecnhmark::generateNumbersSequence,
                    "sumComplex" to complexNumbersBecnhmark::sumComplex,
                    "subComplex" to complexNumbersBecnhmark::subComplex,
                    "classInheritance" to complexNumbersBecnhmark::classInheritance,
                    "categoryMethods" to complexNumbersBecnhmark::categoryMethods,
                    "stringToObjC" to complexNumbersBecnhmark::stringToObjC,
                    "stringFromObjC" to complexNumbersBecnhmark::stringFromObjC,
                    "fft" to complexNumbersBecnhmark::fft,
                    "invertFft" to complexNumbersBecnhmark::invertFft
            )
    )
}

fun main(args: Array<String>) {
    BenchmarksRunner.runBenchmarks(args, { parser: ArgParser ->
        ObjCInteropLauncher(parser.get<Int>("warmup")!!, parser.get<Int>("repeat")!!, parser.get<String>("prefix")!!).launch(parser.getAll<String>("filter"))
    })
}