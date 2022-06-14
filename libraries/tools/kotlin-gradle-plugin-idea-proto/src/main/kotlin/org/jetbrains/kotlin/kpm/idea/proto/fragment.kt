/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmFragment
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmFragmentImpl
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext

internal fun IdeaKpmSerializationContext.IdeaKpmFragmentProto(fragment: IdeaKpmFragment): IdeaKpmFragmentProto {
    return ideaKpmFragmentProto {
        coordinates = IdeaKpmFragmentCoordinatesProto(fragment.coordinates)
        platforms.addAll(fragment.platforms.map { IdeaKpmPlatformProto(it) })
        languageSettings = IdeaKpmLanguageSettingsProto(fragment.languageSettings)
        dependencies.addAll(fragment.dependencies.map { IdeaKpmDependencyProto(it) })
        sourceDirectories.addAll(fragment.contentRoots.map { IdeaKpmContentRootProto(it) })
        extras = IdeaKpmExtrasProto(fragment.extras)
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmFragment(proto: IdeaKpmFragmentProto): IdeaKpmFragment {
    return IdeaKpmFragmentImpl(
        coordinates = IdeaKpmFragmentCoordinates(proto.coordinates),
        platforms = proto.platformsList.map { IdeaKpmPlatform(it) }.toSet(),
        languageSettings = IdeaKpmLanguageSettings(proto.languageSettings),
        dependencies = proto.dependenciesList.mapNotNull { IdeaKpmDependency(it) },
        contentRoots = proto.sourceDirectoriesList.mapNotNull { IdeaKpmContentRoot(it) },
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKpmSerializationContext.IdeaKpmFragment(data: ByteArray): IdeaKpmFragment {
    return IdeaKpmFragment(IdeaKpmFragmentProto.parseFrom(data))
}

internal fun IdeaKpmFragment.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmFragmentProto(this).toByteArray()
}
