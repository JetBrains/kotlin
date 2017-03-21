package org.jetbrains.kotlin.konan

import org.jetbrains.kotlin.backend.konan.Distribution
import org.jetbrains.kotlin.backend.konan.KonanTarget
import org.jetbrains.kotlin.backend.konan.TargetManager
import java.io.File

class CompilerHelper0(val dist: Distribution): Runnable {

    override fun run() {
        val dependencies = mutableListOf<String>()
        dependencies.add(dist.sysRoot)
        dependencies.add(dist.llvmHome)
        if (dist.sysRoot != dist.targetSysRoot) {
            dependencies.add(dist.targetSysRoot)
        }
        if (TargetManager.host == KonanTarget.LINUX) {
            dependencies.add(dist.libGcc)
        }
        DependencyDownloader(File(dist.dependencies), dist.properties.properties, dependencies).run()
    }
}