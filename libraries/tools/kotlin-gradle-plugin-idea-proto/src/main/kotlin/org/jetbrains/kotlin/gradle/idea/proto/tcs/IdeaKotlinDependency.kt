/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:JvmName("IdeaKotlinDependency")

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import com.google.protobuf.InvalidProtocolBufferException
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinDependencyProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinDependencyProto.DependencyCase
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinDependencyProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.tcs.*

internal fun IdeaKotlinSerializationContext.IdeaKotlinDependencyProto(dependency: IdeaKotlinDependency): IdeaKotlinDependencyProto {
    return ideaKotlinDependencyProto {
        when (dependency) {
            is IdeaKotlinResolvedBinaryDependency -> resolvedBinaryDependency = IdeaKotlinResolvedBinaryDependencyProto(dependency)
            is IdeaKotlinUnresolvedBinaryDependency -> unresolvedBinaryDependency = IdeaKotlinUnresolvedBinaryDependencyProto(dependency)
            is IdeaKotlinSourceDependency -> sourceDependency = IdeaKotlinSourceDependencyProto(dependency)
            is IdeaKotlinProjectArtifactDependency -> projectArtifactDependency = IdeaKotlinProjectArtifactDependencyProto(dependency)
        }.let { }
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKotlinDependency(proto: IdeaKotlinDependencyProto): IdeaKotlinDependency? {
    return when (proto.dependencyCase) {
        DependencyCase.SOURCE_DEPENDENCY -> IdeaKotlinSourceDependency(proto.sourceDependency)
        DependencyCase.RESOLVED_BINARY_DEPENDENCY -> IdeaKotlinResolvedBinaryDependency(proto.resolvedBinaryDependency)
        DependencyCase.UNRESOLVED_BINARY_DEPENDENCY -> IdeaKotlinUnresolvedBinaryDependency(proto.unresolvedBinaryDependency)
        DependencyCase.PROJECT_ARTIFACT_DEPENDENCY -> IdeaKotlinProjectArtifactDependency(proto.projectArtifactDependency)
        DependencyCase.DEPENDENCY_NOT_SET -> {
            logger.error("Dependency not set")
            null
        }
        null -> null
    }
}

fun IdeaKotlinDependency.toByteArray(context: IdeaKotlinSerializationContext): ByteArray {
    return context.IdeaKotlinDependencyProto(this).toByteArray()
}

fun IdeaKotlinSerializationContext.IdeaKotlinDependency(data: ByteArray): IdeaKotlinDependency? {
    return try {
        val proto = IdeaKotlinDependencyProto.parseFrom(data)
        IdeaKotlinDependency(proto)

    } catch (e: InvalidProtocolBufferException) {
        logger.error("Failed to deserialize ${IdeaKotlinDependency::class.java.simpleName}", e)
        null
    }
}
