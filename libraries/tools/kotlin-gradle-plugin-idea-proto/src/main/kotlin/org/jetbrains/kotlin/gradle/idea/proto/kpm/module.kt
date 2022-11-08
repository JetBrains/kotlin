/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmModule
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmModuleImpl
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmVariant
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.IdeaKpmModuleProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.ideaKpmModuleProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext

internal fun IdeaSerializationContext.IdeaKpmModuleProto(module: IdeaKpmModule): IdeaKpmModuleProto {
    return ideaKpmModuleProto {
        coordinates = IdeaKpmModuleCoordinatesProto(module.coordinates)
        fragments.addAll(module.fragments.filter { it !is IdeaKpmVariant }.map { IdeaKpmFragmentProto(it) })
        variants.addAll(module.fragments.filterIsInstance<IdeaKpmVariant>().map { IdeaKpmVariantProto(it) })
    }
}

internal fun IdeaSerializationContext.IdeaKpmModule(proto: IdeaKpmModuleProto): IdeaKpmModule {
    return IdeaKpmModuleImpl(
        coordinates = IdeaKpmModuleCoordinates(proto.coordinates),
        fragments = proto.fragmentsList.map { IdeaKpmFragment(it) } + proto.variantsList.map { IdeaKpmVariant(it) }
    )
}

internal fun IdeaSerializationContext.IdeaKpmModule(data: ByteArray): IdeaKpmModule {
    return IdeaKpmModule(IdeaKpmModuleProto.parseFrom(data))
}

internal fun IdeaKpmModule.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmModuleProto(this).toByteArray()
}
