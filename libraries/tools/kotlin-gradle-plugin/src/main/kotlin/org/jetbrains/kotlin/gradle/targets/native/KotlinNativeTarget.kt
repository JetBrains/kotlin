/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.util.ConfigureUtil
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeBinaryTestRun
import org.jetbrains.kotlin.gradle.targets.native.NativeBinaryTestRunSource
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

open class KotlinNativeTarget @Inject constructor(
    project: Project,
    val konanTarget: KonanTarget
) : KotlinOnlyTarget<KotlinNativeCompilation>(project, KotlinPlatformType.native) {

    init {
        attributes.attribute(konanTargetAttribute, konanTarget.name)
    }

    val binaries =
        // Use newInstance to allow accessing binaries by their names in Groovy using the extension mechanism.
        project.objects.newInstance(KotlinNativeBinaryContainer::class.java, this, WrapUtil.toDomainObjectSet(NativeBinary::class.java))

    fun binaries(configure: KotlinNativeBinaryContainer.() -> Unit) {
        binaries.configure()
    }

    fun binaries(configure: Closure<*>) {
        ConfigureUtil.configure(configure, binaries)
    }

    override val artifactsTaskName: String
        get() = disambiguateName("binaries")

    override val publishable: Boolean
        get() = konanTarget.enabledOnCurrentHost

    // User-visible constants
    val DEBUG = NativeBuildType.DEBUG
    val RELEASE = NativeBuildType.RELEASE

    val EXECUTABLE = NativeOutputKind.EXECUTABLE
    val FRAMEWORK = NativeOutputKind.FRAMEWORK
    val DYNAMIC = NativeOutputKind.DYNAMIC
    val STATIC = NativeOutputKind.STATIC

    companion object {
        val konanTargetAttribute = Attribute.of(
            "org.jetbrains.kotlin.native.target",
            String::class.java
        )
    }
}

// TODO: Add separate classes for corresponding targets?
open class KotlinNativeTargetWithTests<T : KotlinNativeBinaryTestRun> @Inject constructor(
    project: Project,
    konanTarget: KonanTarget
) : KotlinNativeTarget(project, konanTarget), KotlinTargetWithTests<NativeBinaryTestRunSource, T> {

    override lateinit var testRuns: NamedDomainObjectContainer<T>
        internal set
}