package org.jetbrains.kotlin.konan

import org.jetbrains.kotlin.backend.konan.Distribution
import java.io.File

class CompilerHelper0(val dist: Distribution): Runnable {

    override fun run() {
        DependencyDownloader(File(dist.dependenciesDir), dist.properties.properties, dist.dependencies).run()
    }
}