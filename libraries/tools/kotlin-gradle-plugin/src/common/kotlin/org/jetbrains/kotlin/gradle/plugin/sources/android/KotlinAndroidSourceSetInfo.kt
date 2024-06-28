/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import com.android.build.gradle.BaseExtension
import com.android.builder.model.SourceProvider
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.*

@ExperimentalKotlinGradlePluginApi
val KotlinSourceSet.androidSourceSetInfoOrNull: KotlinAndroidSourceSetInfo?
    get() = (this as? ExtensionAware)?.extensions?.findByType()

@ExperimentalKotlinGradlePluginApi
var KotlinSourceSet.androidSourceSetInfo: KotlinAndroidSourceSetInfo
    get() = androidSourceSetInfoOrNull ?: throw UnknownDomainObjectException("No 'androidSourceSetInfo' found on KotlinSourceSet: $name")
    internal set(value) {
        (this as ExtensionAware).extensions.add("androidSourceSetInfo", value)
    }

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
@ExperimentalKotlinGradlePluginApi
fun Project.findAndroidSourceSet(kotlinSourceSet: KotlinSourceSet): DeprecatedAndroidSourceSet? {
    val androidSourceSetInfo = kotlinSourceSet.androidSourceSetInfoOrNull ?: return null
    val android = extensions.findByType<BaseExtension>() ?: return null
    return android.sourceSets.getByName(androidSourceSetInfo.androidSourceSetName)
}

@ExperimentalKotlinGradlePluginApi
fun Project.findKotlinSourceSet(
    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") androidSourceSet: DeprecatedAndroidSourceSet
): KotlinSourceSet? {
    return findKotlinSourceSet(androidSourceSet.name)
}

@ExperimentalKotlinGradlePluginApi
fun Project.findKotlinSourceSet(androidSourceSet: SourceProvider): KotlinSourceSet? {
    return findKotlinSourceSet(androidSourceSet.name)
}

private fun Project.findKotlinSourceSet(androidSourceSetName: String): KotlinSourceSet? {
    return kotlinExtensionOrNull?.sourceSets.orEmpty()
        .find { kotlinSourceSet -> kotlinSourceSet.androidSourceSetInfoOrNull?.androidSourceSetName == androidSourceSetName }
}

@ExperimentalKotlinGradlePluginApi
sealed class KotlinAndroidSourceSetInfo {
    abstract val kotlinSourceSetName: String
    abstract val androidSourceSetName: String
    internal abstract val androidVariantType: AndroidVariantType
    abstract val androidVariantNames: Set<String>

    internal data class Mutable(
        override val kotlinSourceSetName: String,
        override val androidSourceSetName: String,
        override var androidVariantType: AndroidVariantType = AndroidVariantType.Unknown,
        override val androidVariantNames: MutableSet<String> = mutableSetOf()
    ) : KotlinAndroidSourceSetInfo()

    internal fun asMutable(): Mutable = when (this) {
        is Mutable -> this
    }
}
