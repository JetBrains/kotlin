/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.saveTo
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*

/**
 * Merges multiple individual KLib ABI dumps into a single merged dump.
 */
@CacheableTask
public abstract class KotlinKlibMergeAbiTask : DefaultTask() {
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

    @OptIn(ExperimentalBCVApi::class)
    @TaskAction
    internal fun merge() {
        KlibDump().apply {
            dumps.get().forEach { dump ->
                val dumpFile = dump.dumpFile.asFile.get()
                if (dumpFile.exists()) {
                    merge(dumpFile, dump.target.configurableName)
                }
            }
        }.saveTo(mergedApiFile.asFile.get())
    }
}
