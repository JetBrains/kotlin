/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmCompilationOutput
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmCompilationOutputImpl
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
