/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmVariant
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmVariantImpl
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext


internal fun IdeaSerializationContext.IdeaKpmVariantProto(variant: IdeaKpmVariant): IdeaKpmVariantProto {
    return ideaKpmVariantProto {
        fragment = IdeaKpmFragmentProto(variant)
        variantAttributes.putAll(variant.variantAttributes)
        platform = IdeaKpmPlatformProto(variant.platform)
        compilationOutput = IdeaKpmCompilationOutputProto(variant.compilationOutputs)
    }
}

internal fun IdeaSerializationContext.IdeaKpmVariant(proto: IdeaKpmVariantProto): IdeaKpmVariant {
    return IdeaKpmVariantImpl(
        fragment = IdeaKpmFragment(proto.fragment),
        platform = IdeaKpmPlatform(proto.platform),
        variantAttributes = proto.variantAttributesMap.toMap(),
        compilationOutputs = IdeaKpmCompilationOutput(proto.compilationOutput)
    )
}

internal fun IdeaSerializationContext.IdeaKpmVariant(data: ByteArray): IdeaKpmVariant {
    return IdeaKpmVariant(IdeaKpmVariantProto.parseFrom(data))
}

internal fun IdeaKpmVariant.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmVariantProto(this).toByteArray()
}
