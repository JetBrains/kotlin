/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.konan.target.allTargetsWithSanitizers
import org.jetbrains.kotlin.konan.util.DependencyDirectories

plugins {
    id("native-dependencies-downloader")
    id("native-dependencies")
}

nativeDependenciesDownloader {
    repositoryURL.set("https://cache-redirector.jetbrains.com/download.jetbrains.com/kotlin/native")
    dependenciesDirectory.set(DependencyDirectories.getDependenciesRoot(kotlinBuildProperties.konanDataDir))

    allTargets {}
}

/**
 * Download all dependencies.
 */
val update by tasks.registering {
    platformManager.allTargetsWithSanitizers.forEach {
        dependsOn(nativeDependencies.targetDependency(it))
    }
}

// TODO: This sort of task probably belongs to :kotlin-native
val rmDotKonan by tasks.registering(Delete::class) {
    val dir = System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}/.konan"
    delete(dir)
}
