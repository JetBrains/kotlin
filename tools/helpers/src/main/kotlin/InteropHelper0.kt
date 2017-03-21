package org.jetbrains.kotlin.konan

import org.jetbrains.kotlin.backend.konan.KonanProperties
import java.io.File
import java.util.*

class InteropHelper0(
        val dependenciesRoot: String,
        val propertiesFile: String,
        val dependencies: List<String>): Runnable {

    override fun run() =
            DependencyDownloader(
                    File(dependenciesRoot),
                    Properties().apply { load(File(propertiesFile).inputStream()) },
                    dependencies
            ).run()
}
