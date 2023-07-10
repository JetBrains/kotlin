/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinBinaryCapabilityProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinBinaryCapabilityProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCapability

internal fun IdeaKotlinBinaryCapabilityProto(
    capability: IdeaKotlinBinaryCapability
): IdeaKotlinBinaryCapabilityProto {
    return ideaKotlinBinaryCapabilityProto {
        this.group = capability.group
        this.name = capability.name
        capability.version?.let { this.version = it }
    }
}

internal fun IdeaKotlinBinaryCapability(proto: IdeaKotlinBinaryCapabilityProto): IdeaKotlinBinaryCapability {
    return IdeaKotlinBinaryCapability(
        group = proto.group,
        name = proto.name,
        version = if (proto.hasVersion()) proto.version else null,
    )
}
