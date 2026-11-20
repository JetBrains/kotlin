/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.icevents

import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.trackers.FileCompilationReason

@Serializable
internal class FileCompilationReasonDto(
    override val readableString: String,
) : FileCompilationReason.Basic

@Serializable
internal class CompileIterationDto(
    override val reasons: Map<String, List<FileCompilationReasonDto>>,
    override val exitCode: String,
    override val timestamp: Long,
    override val iteration: Int
) : IcEvent.CompileIteration {
    override val type = "CompileIteration"
}

@Serializable
internal class CompilationStartedDto(
    override val isIncremental: Boolean,
    override val reason: String?,
    override val timestamp: Long,
    override val iteration: Int
) : IcEvent.CompilationStarted {
    override val type = "CompilationStarted"
}
