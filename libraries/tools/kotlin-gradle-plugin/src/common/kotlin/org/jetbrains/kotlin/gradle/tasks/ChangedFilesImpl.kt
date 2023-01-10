/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.incremental.ChangedFiles as ChangedFilesInternal
import java.io.File

internal abstract class ChangedFilesImpl : ChangedFiles {
    companion object {
        fun from(changedFiles: ChangedFilesInternal): ChangedFiles =
            when (changedFiles) {
                is ChangedFilesInternal.Known -> KnownImpl(changedFiles.modified, changedFiles.removed)
                is ChangedFilesInternal.Unknown -> UnknownImpl()
            }
    }

    class KnownImpl(
        override val modified: List<File>,
        override val removed: List<File>
    ) : ChangedFiles.Known

    class UnknownImpl : ChangedFiles.Unknown
}

fun <T: CommonCompilerArguments> T.updateByChangedFiles(
    transformers: List<(ChangedFiles) -> Map<String, List<SubpluginOption>>>,
    changedFilesInternal: ChangedFilesInternal
): T {
    val changedFiles = ChangedFilesImpl.from(changedFilesInternal)
    val extraOptions = CompilerPluginOptions()
    transformers.forEach { transform ->
        transform(changedFiles).forEach { (pluginId, options) ->
            options.forEach { option ->
                extraOptions.addPluginArgument(pluginId, option)
            }
        }
    }
    pluginOptions = ((pluginOptions?.toList() ?: emptyList()) + extraOptions.arguments).toTypedArray()
    return this
}
