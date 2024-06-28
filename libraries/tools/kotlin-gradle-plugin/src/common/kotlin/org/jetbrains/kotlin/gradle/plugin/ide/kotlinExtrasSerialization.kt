/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.idea.proto.tcs.IdeaKotlinClasspathSerializer
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinBooleanExtrasSerializer
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializer.Companion.javaIoSerializable
import org.jetbrains.kotlin.gradle.idea.tcs.extras.*

@InternalKotlinGradlePluginApi
val kotlinExtrasSerialization = IdeaKotlinExtrasSerializationExtension {
    register(kotlinDebugKey, javaIoSerializable())
    register(KlibExtra.key, javaIoSerializable())
    register(isIdeaProjectLevelKey, IdeaKotlinBooleanExtrasSerializer)
    register(isNativeDistributionKey, IdeaKotlinBooleanExtrasSerializer)
    register(isNativeStdlibKey, IdeaKotlinBooleanExtrasSerializer)
    register(isCommonizedKey, IdeaKotlinBooleanExtrasSerializer)
    register(sourcesClasspathKey, IdeaKotlinClasspathSerializer)
    register(documentationClasspathKey, IdeaKotlinClasspathSerializer)
    register(projectArtifactsClasspathKey, IdeaKotlinClasspathSerializer)
    register(isOpaqueFileDependencyKey, IdeaKotlinBooleanExtrasSerializer)
}
