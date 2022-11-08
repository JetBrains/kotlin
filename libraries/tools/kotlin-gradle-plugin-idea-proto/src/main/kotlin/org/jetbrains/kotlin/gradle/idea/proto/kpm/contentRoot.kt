/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmContentRoot
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmContentRootImpl
import org.jetbrains.kotlin.gradle.idea.proto.Extras
import org.jetbrains.kotlin.gradle.idea.proto.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.IdeaKpmContentRootProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.ideaKpmContentRootProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext
import java.io.File

internal fun IdeaSerializationContext.IdeaKpmContentRootProto(sourceDirectory: IdeaKpmContentRoot): IdeaKpmContentRootProto {
    return ideaKpmContentRootProto {
        absolutePath = sourceDirectory.file.absolutePath
        type = sourceDirectory.type
        extras = IdeaExtrasProto(sourceDirectory.extras)
    }
}

internal fun IdeaSerializationContext.IdeaKpmContentRoot(proto: IdeaKpmContentRootProto): IdeaKpmContentRoot {
    return IdeaKpmContentRootImpl(
        file = File(proto.absolutePath),
        type = proto.type,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaSerializationContext.IdeaKpmContentRoot(data: ByteArray): IdeaKpmContentRoot {
    return IdeaKpmContentRoot(IdeaKpmContentRootProto.parseFrom(data))
}

internal fun IdeaKpmContentRoot.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmContentRootProto(this).toByteArray()
}
