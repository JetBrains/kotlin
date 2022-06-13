/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmModule
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmModuleImpl
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmVariant
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext

internal fun IdeaKpmSerializationContext.IdeaKpmModuleProto(module: IdeaKpmModule): IdeaKpmModuleProto {
    return ideaKpmModuleProto {
        coordinates = IdeaKpmModuleCoordinatesProto(module.coordinates)
        fragments.addAll(module.fragments.filter { it !is IdeaKpmVariant }.map { IdeaKpmFragmentProto(it) })
        variants.addAll(module.fragments.filterIsInstance<IdeaKpmVariant>().map { IdeaKpmVariantProto(it) })
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmModule(proto: IdeaKpmModuleProto): IdeaKpmModule {
    return IdeaKpmModuleImpl(
        coordinates = IdeaKpmModuleCoordinates(proto.coordinates),
        fragments = proto.fragmentsList.map { IdeaKpmFragment(it) } + proto.variantsList.map { IdeaKpmVariant(it) }
    )
}

internal fun IdeaKpmSerializationContext.IdeaKpmModule(data: ByteArray): IdeaKpmModule {
    return IdeaKpmModule(IdeaKpmModuleProto.parseFrom(data))
}

internal fun IdeaKpmModule.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmModuleProto(this).toByteArray()
}
