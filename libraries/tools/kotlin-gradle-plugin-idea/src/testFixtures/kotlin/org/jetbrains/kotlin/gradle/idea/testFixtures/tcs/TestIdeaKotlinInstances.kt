/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.tcs

import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.extrasOf
import org.jetbrains.kotlin.tooling.core.toMutableExtras
import org.jetbrains.kotlin.tooling.core.withValue
import java.io.File

object TestIdeaKotlinInstances {

    val extrasWithIntAndStrings = extrasOf(
        extrasKeyOf<Int>() withValue 1,
        extrasKeyOf<String>() withValue "Cash"
    )

    @Suppress("DEPRECATION")
    val simpleProjectCoordinates = IdeaKotlinProjectCoordinates(
        buildId = "myBuildId",
        projectPath = "my:project:path",
        projectName = "myProjectName"
    )

    val simpleBinaryCoordinates = IdeaKotlinBinaryCoordinates(
        group = "myGroup",
        module = "myModule",
        version = "myVersion",
        sourceSetName = "mySourceSetName"
    )

    val simpleSourceCoordinates = IdeaKotlinSourceCoordinates(
        project = simpleProjectCoordinates,
        sourceSetName = "mySourceSetName"
    )

    val simpleUnresolvedBinaryDependency = IdeaKotlinUnresolvedBinaryDependency(
        cause = "myCause",
        coordinates = simpleBinaryCoordinates,
        extras = extrasWithIntAndStrings.toMutableExtras()
    )

    val simpleClasspath = IdeaKotlinClasspath(setOf(File("myFirstFile.klib"), File("mySecondFile.jar").absoluteFile))

    val emptyClasspath = IdeaKotlinClasspath()

    val simpleResolvedBinaryDependency = IdeaKotlinResolvedBinaryDependency(
        binaryType = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
        classpath = simpleClasspath,
        coordinates = simpleBinaryCoordinates,
        extras = extrasWithIntAndStrings.toMutableExtras()
    )

    val simpleSourceDependency = IdeaKotlinSourceDependency(
        coordinates = simpleSourceCoordinates,
        type = IdeaKotlinSourceDependency.Type.Regular,
        extras = extrasWithIntAndStrings.toMutableExtras()
    )

    val simpleProjectArtifactDependency = IdeaKotlinProjectArtifactDependency(
        coordinates = simpleProjectCoordinates,
        type = IdeaKotlinSourceDependency.Type.Regular,
        extras = extrasWithIntAndStrings.toMutableExtras()
    )
}
