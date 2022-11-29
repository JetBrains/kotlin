/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmFragment
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmFragmentImpl
import org.jetbrains.kotlin.gradle.idea.proto.Extras
import org.jetbrains.kotlin.gradle.idea.proto.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.IdeaKpmFragmentProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.ideaKpmFragmentProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext

internal fun IdeaKotlinSerializationContext.IdeaKpmFragmentProto(fragment: IdeaKpmFragment): IdeaKpmFragmentProto {
    return ideaKpmFragmentProto {
        coordinates = IdeaKpmFragmentCoordinatesProto(fragment.coordinates)
        platforms.addAll(fragment.platforms.map { IdeaKpmPlatformProto(it) })
        languageSettings = IdeaKpmLanguageSettingsProto(fragment.languageSettings)
        dependencies.addAll(fragment.dependencies.map { IdeaKpmDependencyProto(it) })
        sourceDirectories.addAll(fragment.contentRoots.map { IdeaKpmContentRootProto(it) })
        extras = IdeaExtrasProto(fragment.extras)
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKpmFragment(proto: IdeaKpmFragmentProto): IdeaKpmFragment {
    return IdeaKpmFragmentImpl(
        coordinates = IdeaKpmFragmentCoordinates(proto.coordinates),
        platforms = proto.platformsList.map { IdeaKpmPlatform(it) }.toSet(),
        languageSettings = IdeaKpmLanguageSettings(proto.languageSettings),
        dependencies = proto.dependenciesList.mapNotNull { IdeaKpmDependency(it) },
        contentRoots = proto.sourceDirectoriesList.mapNotNull { IdeaKpmContentRoot(it) },
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKotlinSerializationContext.IdeaKpmFragment(data: ByteArray): IdeaKpmFragment {
    return IdeaKpmFragment(IdeaKpmFragmentProto.parseFrom(data))
}

internal fun IdeaKpmFragment.toByteArray(context: IdeaKotlinSerializationContext): ByteArray {
    return context.IdeaKpmFragmentProto(this).toByteArray()
}
