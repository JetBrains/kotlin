/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmContentRoot
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmContentRootImpl
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext
import java.io.File

internal fun IdeaKpmSerializationContext.IdeaKpmContentRootProto(sourceDirectory: IdeaKpmContentRoot): IdeaKpmContentRootProto {
    return ideaKpmContentRootProto {
        absolutePath = sourceDirectory.file.absolutePath
        type = sourceDirectory.type
        extras = IdeaKpmExtrasProto(sourceDirectory.extras)
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmContentRoot(proto: IdeaKpmContentRootProto): IdeaKpmContentRoot {
    return IdeaKpmContentRootImpl(
        file = File(proto.absolutePath),
        type = proto.type,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKpmSerializationContext.IdeaKpmContentRoot(data: ByteArray): IdeaKpmContentRoot {
    return IdeaKpmContentRoot(IdeaKpmContentRootProto.parseFrom(data))
}

internal fun IdeaKpmContentRoot.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmContentRootProto(this).toByteArray()
}
