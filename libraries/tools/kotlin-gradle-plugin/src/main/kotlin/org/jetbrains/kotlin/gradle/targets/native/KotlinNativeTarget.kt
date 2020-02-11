/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeBinaryTestRun
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeHostTestRun
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeSimulatorTestRun
import org.jetbrains.kotlin.gradle.targets.native.NativeBinaryTestRunSource
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

open class KotlinNativeTarget @Inject constructor(
    project: Project,
    val konanTarget: KonanTarget
) : KotlinBinaryContainer<KotlinNativeCompilation, KotlinNativeBinaryContainer>(
    project,
    KotlinPlatformType.native
) {

    init {
        attributes.attribute(konanTargetAttribute, konanTarget.name)
    }

    override val binaries =
        // Use newInstance to allow accessing binaries by their names in Groovy using the extension mechanism.
        project.objects.newInstance(KotlinNativeBinaryContainer::class.java, this, WrapUtil.toDomainObjectSet(NativeBinary::class.java))

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

abstract class KotlinNativeTargetWithTests<T : KotlinNativeBinaryTestRun>(
    project: Project,
    konanTarget: KonanTarget
) : KotlinNativeTarget(project, konanTarget), KotlinTargetWithTests<NativeBinaryTestRunSource, T> {

    override lateinit var testRuns: NamedDomainObjectContainer<T>
        internal set
}

open class KotlinNativeTargetWithHostTests @Inject constructor(project: Project, konanTarget: KonanTarget) :
    KotlinNativeTargetWithTests<KotlinNativeHostTestRun>(project, konanTarget)

open class KotlinNativeTargetWithSimulatorTests @Inject constructor(project: Project, konanTarget: KonanTarget) :
    KotlinNativeTargetWithTests<KotlinNativeSimulatorTestRun>(project, konanTarget)