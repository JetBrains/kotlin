/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmCompilationOutput
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmCompilationOutputImpl
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.IdeaKpmCompilationOutputProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.ideaKpmCompilationOutputProto
import java.io.File

internal fun IdeaKpmCompilationOutputProto(output: IdeaKpmCompilationOutput): IdeaKpmCompilationOutputProto {
    return ideaKpmCompilationOutputProto {
        classesDirs.addAll(output.classesDirs.map { it.absolutePath })
        output.resourcesDir?.absolutePath?.let { this.resourcesDir = it }
    }
}


internal fun IdeaKpmCompilationOutput(proto: IdeaKpmCompilationOutputProto): IdeaKpmCompilationOutput {
    return IdeaKpmCompilationOutputImpl(
        classesDirs = proto.classesDirsList.map { File(it) }.toSet(),
        resourcesDir = if (proto.hasResourcesDir()) File(proto.resourcesDir) else null
    )
}

internal fun IdeaKpmCompilationOutput(data: ByteArray): IdeaKpmCompilationOutput {
    return IdeaKpmCompilationOutput(IdeaKpmCompilationOutputProto.parseFrom(data))
}

internal fun IdeaKpmCompilationOutput.toByteArray(): ByteArray {
    return IdeaKpmCompilationOutputProto(this).toByteArray()
}
