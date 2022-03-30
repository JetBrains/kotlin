/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinModuleCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinModuleCoordinatesImpl
import org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi

internal fun ModuleCoordinatesProto(coordinates: IdeaKotlinModuleCoordinates): ModuleCoordinatesProto {
    return moduleCoordinatesProto {
        buildId = coordinates.buildId
        projectPath = coordinates.projectPath
        projectName = coordinates.projectName
        moduleName = coordinates.moduleName
        coordinates.moduleClassifier?.let { moduleClassifier = it }
    }
}

fun IdeaKotlinModuleCoordinates.serialize(): ByteArray {
    return ModuleCoordinatesProto(this).toByteArray()
}

fun IdeaKotlinModuleCoordinates(data: ByteArray): IdeaKotlinModuleCoordinates {
    return IdeaKotlinModuleCoordinates(ModuleCoordinatesProto.parseFrom(data))
}

internal fun IdeaKotlinModuleCoordinates(proto: ModuleCoordinatesProto): IdeaKotlinModuleCoordinates {
    return IdeaKotlinModuleCoordinatesImpl(
        buildId = proto.buildId,
        projectPath = proto.projectPath,
        projectName = proto.projectName,
        moduleName = proto.moduleName,
        moduleClassifier = if (proto.hasModuleClassifier()) proto.moduleClassifier else null
    )
}
