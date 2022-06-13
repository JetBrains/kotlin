/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProject
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProjectImpl
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext
import java.io.File

internal fun IdeaKpmSerializationContext.IdeaKpmProjectProto(project: IdeaKpmProject): IdeaKpmProjectProto {
    return ideaKpmProjectProto {
        gradlePluginVersion = project.gradlePluginVersion
        coreLibrariesVersion = project.coreLibrariesVersion
        project.explicitApiModeCliOption?.let { explicitApiModeCliOption = it }
        kotlinNativeHome = project.kotlinNativeHome.absolutePath
        modules.addAll(project.modules.map { IdeaKpmModuleProto(it) })
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmProject(proto: IdeaKpmProjectProto): IdeaKpmProject {
    return IdeaKpmProjectImpl(
        gradlePluginVersion = proto.gradlePluginVersion,
        coreLibrariesVersion = proto.coreLibrariesVersion,
        explicitApiModeCliOption = if (proto.hasExplicitApiModeCliOption()) proto.explicitApiModeCliOption else null,
        kotlinNativeHome = File(proto.kotlinNativeHome),
        modules = proto.modulesList.map { IdeaKpmModule(it) }
    )
}
