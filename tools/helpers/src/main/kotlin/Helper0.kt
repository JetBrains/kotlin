package org.jetbrains.kotlin.konan

import java.io.File
import java.util.*

class Helper0(val dependenciesDir: String,
              val properties: Properties,
              val dependencies: List<String>): Runnable {

    override fun run() {
        DependencyDownloader(File(dependenciesDir), properties, dependencies).run()
    }
}