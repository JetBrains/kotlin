package org.jetbrains.kotlin.konan

import java.io.File
import java.util.*

// TODO: Add command line keys
// Args: dependencies directory, dependencies url, dependencies list
fun main(args: Array<String>) {
    if (args.size < 2) {
        System.exit(1)
    }
    val dependenciesDir = File(args[0])
    val dependenciesUrl = args[1]
    val dependencies = List<String>(args.size - 2) { args[2 + it] }
    DependencyDownloader(dependenciesDir, dependenciesUrl, dependencies).run()
}
