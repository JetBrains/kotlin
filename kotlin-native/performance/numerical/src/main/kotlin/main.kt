/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the licenses/LICENSE.txt file.
 */

import org.jetbrains.benchmarksLauncher.*
import kotlinx.cli.*

expect class NumericalLauncher() : Launcher {
    override val baseBenchmarksSet: MutableMap<String, AbstractBenchmarkEntry>
}

fun main(args: Array<String>) {
    val launcher = NumericalLauncher()
    BenchmarksRunner.runBenchmarks(args, { arguments: BenchmarkArguments ->
        if (arguments is BaseBenchmarkArguments) {
            launcher.launch(arguments.warmup, arguments.repeat, arguments.prefix,
                    arguments.filter, arguments.filterRegex, arguments.verbose)
        } else emptyList()
    }, benchmarksListAction = launcher::benchmarksListAction)
}