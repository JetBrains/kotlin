/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.startup.*
import org.jetbrains.benchmarksLauncher.*
import kotlinx.cli.*

class StartupLauncher : Launcher() {
    override val benchmarks = BenchmarksCollection(
      mutableMapOf(
          "Singleton.initialize" to BenchmarkEntryManual(::singletonInitialize),
          "Singleton.initializeNested" to BenchmarkEntryManual(::singletonInitializeNested),
      )
    )
}

fun main(args: Array<String>) {
    val launcher = StartupLauncher()
    BenchmarksRunner.runBenchmarks(args, { arguments: BenchmarkArguments ->
        if (arguments is BaseBenchmarkArguments) {
            launcher.launch(arguments.warmup, arguments.repeat, arguments.prefix,
                    arguments.filter, arguments.filterRegex, arguments.verbose)
        } else emptyList()
    }, benchmarksListAction = launcher::benchmarksListAction)
}
