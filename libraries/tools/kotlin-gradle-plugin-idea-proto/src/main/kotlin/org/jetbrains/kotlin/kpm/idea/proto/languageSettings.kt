/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmLanguageSettings
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmLanguageSettingsImpl
import java.io.File

internal fun IdeaKpmLanguageSettingsProto(languageSettings: IdeaKpmLanguageSettings): IdeaKpmLanguageSettingsProto {
    return ideaKpmLanguageSettingsProto {
        languageSettings.languageVersion?.let { languageVersion = it }
        languageSettings.apiVersion?.let { apiVersion = it }
        isProgressiveMode = languageSettings.isProgressiveMode
        enabledLanguageFeatures.addAll(languageSettings.enabledLanguageFeatures)
        optInAnnotationsInUse.addAll(languageSettings.optInAnnotationsInUse)
        compilerPluginArguments.addAll(languageSettings.compilerPluginArguments)
        compilerPluginClasspath.addAll(languageSettings.compilerPluginClasspath.map { it.absolutePath })
        freeCompilerArgs.addAll(languageSettings.freeCompilerArgs)
    }
}

internal fun IdeaKpmLanguageSettings(proto: IdeaKpmLanguageSettingsProto): IdeaKpmLanguageSettings {
    return IdeaKpmLanguageSettingsImpl(
        languageVersion = if (proto.hasLanguageVersion()) proto.languageVersion else null,
        apiVersion = if (proto.hasApiVersion()) proto.apiVersion else null,
        isProgressiveMode = proto.isProgressiveMode,
        enabledLanguageFeatures = proto.enabledLanguageFeaturesList.toSet(),
        optInAnnotationsInUse = proto.optInAnnotationsInUseList.toSet(),
        compilerPluginArguments = proto.compilerPluginArgumentsList.toList(),
        compilerPluginClasspath = proto.compilerPluginClasspathList.map { File(it) },
        freeCompilerArgs = proto.freeCompilerArgsList.toList()
    )
}

internal fun IdeaKpmLanguageSettings(data: ByteArray): IdeaKpmLanguageSettings {
    return IdeaKpmLanguageSettings(IdeaKpmLanguageSettingsProto.parseFrom(data))
}

internal fun IdeaKpmLanguageSettings.toByteArray(): ByteArray {
    return IdeaKpmLanguageSettingsProto(this).toByteArray()
}

