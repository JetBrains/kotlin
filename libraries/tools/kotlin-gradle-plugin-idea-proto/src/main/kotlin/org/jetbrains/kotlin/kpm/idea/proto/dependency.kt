/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmDependency
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmFragmentDependency
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmUnresolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext
import org.jetbrains.kotlin.gradle.kpm.idea.*
import org.jetbrains.kotlin.kpm.idea.proto.IdeaKpmDependencyProto.DependencyCase

internal fun IdeaSerializationContext.IdeaKpmDependencyProto(dependency: IdeaKpmDependency): IdeaKpmDependencyProto {
    return ideaKpmDependencyProto {
        when (dependency) {
            is IdeaKpmResolvedBinaryDependency -> resolvedBinaryDependency = IdeaKpmResolvedBinaryDependencyProto(dependency)
            is IdeaKpmUnresolvedBinaryDependency -> unresolvedBinaryDependency = IdeaKpmUnresolvedBinaryDependencyProto(dependency)
            is IdeaKpmFragmentDependency -> fragmentDependency = IdeaKpmFragmentDependencyProto(dependency)
        }
    }
}

internal fun IdeaSerializationContext.IdeaKpmDependency(proto: IdeaKpmDependencyProto): IdeaKpmDependency? {
    return when (proto.dependencyCase) {
        DependencyCase.UNRESOLVED_BINARY_DEPENDENCY -> IdeaKpmUnresolvedBinaryDependency(proto.unresolvedBinaryDependency)
        DependencyCase.RESOLVED_BINARY_DEPENDENCY -> IdeaKpmResolvedBinaryDependency(proto.resolvedBinaryDependency)
        DependencyCase.FRAGMENT_DEPENDENCY -> IdeaKpmFragmentDependency(proto.fragmentDependency)
        DependencyCase.DEPENDENCY_NOT_SET, null -> null
    }
}
