/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceDependencyProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency

internal fun IdeaKotlinSerializationContext.IdeaKotlinSourceDependencyType(type: IdeaKotlinSourceDependencyProto.Type?): IdeaKotlinSourceDependency.Type {
    return when (type) {
        IdeaKotlinSourceDependencyProto.Type.REGULAR -> IdeaKotlinSourceDependency.Type.Regular
        IdeaKotlinSourceDependencyProto.Type.FRIEND -> IdeaKotlinSourceDependency.Type.Friend
        IdeaKotlinSourceDependencyProto.Type.DEPENDS_ON -> IdeaKotlinSourceDependency.Type.DependsOn
        IdeaKotlinSourceDependencyProto.Type.UNRECOGNIZED, null -> {
            logger.warn("Unexpected ${IdeaKotlinSourceDependencyProto.Type::class.java.name}: $type")
            IdeaKotlinSourceDependency.Type.Regular
        }
    }
}

internal fun IdeaKotlinSourceDependency.Type.toProto(): IdeaKotlinSourceDependencyProto.Type {
    return when (this) {
        IdeaKotlinSourceDependency.Type.Regular -> IdeaKotlinSourceDependencyProto.Type.REGULAR
        IdeaKotlinSourceDependency.Type.Friend -> IdeaKotlinSourceDependencyProto.Type.FRIEND
        IdeaKotlinSourceDependency.Type.DependsOn -> IdeaKotlinSourceDependencyProto.Type.DEPENDS_ON
    }
}