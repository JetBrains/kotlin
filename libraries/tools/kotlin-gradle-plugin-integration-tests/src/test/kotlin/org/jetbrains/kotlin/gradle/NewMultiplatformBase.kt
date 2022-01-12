/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.native.MPPNativeTargets
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class NewMultiplatformBase : BaseGradleIT() {
    internal val nativeHostTargetName = MPPNativeTargets.current
    internal val unsupportedNativeTargets = MPPNativeTargets.unsupported

    internal fun Project.targetClassesDir(targetName: String, sourceSetName: String = "main") =
        classesDir(sourceSet = "$targetName/$sourceSetName")

    internal data class HmppFlags(
        val hmppSupport: Boolean,
        val enableCompatibilityMetadataArtifact: Boolean,
        val name: String
    ) {
        override fun toString() = name
    }

    internal val noHMPP = HmppFlags(
        name = "No HMPP",
        hmppSupport = false,
        enableCompatibilityMetadataArtifact = false
    )

    internal val hmppWoCompatibilityMetadataArtifact = HmppFlags(
        name = "HMPP without Compatibility Metadata Artifact",
        hmppSupport = true,
        enableCompatibilityMetadataArtifact = false
    )

    internal val hmppWithCompatibilityMetadataArtifact = HmppFlags(
        name = "HMPP with Compatibility Metadata Artifact",
        hmppSupport = true,
        enableCompatibilityMetadataArtifact = true
    )

    internal val HmppFlags.buildOptions
        get() = defaultBuildOptions().copy(
            hierarchicalMPPStructureSupport = hmppSupport,
            enableCompatibilityMetadataVariant = enableCompatibilityMetadataArtifact
        )

    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(stopDaemons = false)
    }

    @BeforeEach
    fun before() {
        super.setUp()
    }

    @AfterEach
    fun after() {
        super.tearDown()
    }
}