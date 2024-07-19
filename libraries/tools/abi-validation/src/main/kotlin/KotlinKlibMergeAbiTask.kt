/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.saveTo
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

/**
 * Merges multiple individual KLib ABI dumps into a single merged dump.
 */
@CacheableTask
public abstract class KotlinKlibMergeAbiTask : WorkerAwareTaskBase() {
    /**
     * Dumps to merge.
     *
     * If there is no dump for a particular target, its [KlibDumpMetadata.dumpFile] won't exist.
     */
    @get:Nested
    public abstract val dumps: SetProperty<KlibDumpMetadata>

    /**
     * A path to a resulting merged dump file.
     */
    @get:OutputFile
    public abstract val mergedApiFile: RegularFileProperty

    @get:Inject
    public abstract val executor: WorkerExecutor

    @TaskAction
    internal fun merge() {
        val q = executor.classLoaderIsolation {
            it.classpath.from(runtimeClasspath)
        }
        q.submit(KlibMergeAbiWorker::class.java) { params ->
            params.dumps.set(dumps.get().map {
                KlibMetadataLocal(it.target, it.dumpFile.get().asFile)
            })
            params.mergedApiFile.set(mergedApiFile)
        }
        q.await()
    }
}

internal interface KlibMergeAbiParameters : WorkParameters {
    val dumps: SetProperty<KlibMetadataLocal>
    val mergedApiFile: RegularFileProperty
}

internal abstract class KlibMergeAbiWorker : WorkAction<KlibMergeAbiParameters> {
    @OptIn(ExperimentalBCVApi::class)
    override fun execute() {
        KlibDump().apply {
            parameters.dumps.get().forEach { dump ->
                val dumpFile = dump.dumpFile
                if (dumpFile.exists()) {
                    merge(dumpFile, dump.target.configurableName)
                }
            }
        }.saveTo(parameters.mergedApiFile.asFile.get())
    }
}
