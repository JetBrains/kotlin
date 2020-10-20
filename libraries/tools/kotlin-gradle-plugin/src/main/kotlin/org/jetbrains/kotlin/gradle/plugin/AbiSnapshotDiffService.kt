/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.incremental.*

abstract class AbiSnapshotDiffService() : BuildService<AbiSnapshotDiffService.Parameters> {
    abstract class Parameters : BuildServiceParameters {
        abstract val caches: IncrementalCachesManager<*>
        abstract val reporter: BuildReporter
        abstract val sourceFilesExtensions: List<String>
    }

    val caches: IncrementalCachesManager<*> = parameters.caches
    val reporter: BuildReporter = parameters.reporter
    val sourceFilesExtensions: List<String> = parameters.sourceFilesExtensions

    companion object {
//        //Store list of changed lookups
//        val diffCache: MutableMap<Pair<AbiSnapshot, AbiSnapshot>, DirtyFilesContainer> = mutableMapOf()
//        @TestOnly
//        fun compareJarsInternal(caches: IncrementalCachesManager<*>, reporter: ICReporter, sourceFilesExtensions: List<String>,
//                                snapshot: AbiSnapshot, newJar: AbiSnapshot) =
//            diffCache.computeIfAbsent(Pair(snapshot, newJar)) { (snapshotJar, actualJar) ->
//                DirtyFilesContainer(caches, reporter, sourceFilesExtensions)
//                    .also {
//                        it.addByDirtyClasses(snapshotJar.fqNames.minus(actualJar.fqNames))
//                        it.addByDirtyClasses(actualJar.fqNames.minus(snapshotJar.fqNames))
//                        it.addByDirtySymbols(snapshotJar.symbols.minus(actualJar.symbols))
//                        it.addByDirtySymbols(actualJar.symbols.minus(snapshotJar.symbols))
//                    }
//            }
    }

//    @Synchronized
//    fun compareJarsInternal(snapshot: AbiSnapshot, newJar: AbiSnapshot): DirtyFilesContainer {
//        return diffCache.computeIfAbsent(Pair(snapshot, newJar)) { (snapshotJar, actualJar) ->
//            DirtyFilesContainer(caches, reporter, sourceFilesExtensions)
//                .also {
//                    it.addByDirtyClasses(snapshotJar.fqNames.minus(actualJar.fqNames))
//                    it.addByDirtyClasses(actualJar.fqNames.minus(snapshotJar.fqNames))
//                    it.addByDirtySymbols(snapshotJar.symbols.minus(actualJar.symbols))
//                    it.addByDirtySymbols(actualJar.symbols.minus(snapshotJar.symbols))
//                }
//
//        }
//    }
}