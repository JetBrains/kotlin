package org.jetbrains.kotlin.konan

import java.io.File
import java.util.*

// TODO: Add command line keys
fun main(args: Array<String>) {
    if (args.size < 2) {
        System.exit(1)
    }
    val dependenciesDir = File(args[0])
    val properties = Properties().apply { load(File(args[1]).inputStream()) }
    val dependencies = List<String>(args.size - 2) { args[2 + it] }
    DependencyDownloader(dependenciesDir, properties, dependencies).run()
}
