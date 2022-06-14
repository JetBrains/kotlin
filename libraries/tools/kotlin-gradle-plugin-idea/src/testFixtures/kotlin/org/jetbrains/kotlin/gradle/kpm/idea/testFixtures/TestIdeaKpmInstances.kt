/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.*
import org.jetbrains.kotlin.tooling.core.emptyExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.extrasOf
import org.jetbrains.kotlin.tooling.core.withValue
import java.io.File

object TestIdeaKpmInstances {

    val extrasWithIntAndStrings = extrasOf(
        extrasKeyOf<Int>() withValue 1,
        extrasKeyOf<String>() withValue "Cash"
    )

    val simpleModuleCoordinates = IdeaKpmModuleCoordinatesImpl(
        buildId = "myBuildId",
        projectPath = "myProjectPath",
        projectName = "myProjectName",
        moduleName = "myModuleName",
        moduleClassifier = "myModuleClassifier"
    )

    val simpleFragmentCoordinates = IdeaKpmFragmentCoordinatesImpl(
        module = simpleModuleCoordinates,
        fragmentName = "myFragmentName"
    )

    val simpleJvmPlatform = IdeaKpmJvmPlatformImpl(
        jvmTarget = "myJvmTarget"
    )

    val simpleLanguageSettings = IdeaKpmLanguageSettingsImpl(
        languageVersion = "myLanguageVersion",
        apiVersion = "myApiVersion",
        isProgressiveMode = true,
        enabledLanguageFeatures = setOf("myFeature1", "myFeature2"),
        optInAnnotationsInUse = setOf("myOptIn1", "myOptIn2"),
        compilerPluginArguments = listOf("myCompilerPluginArgument1", "myCompilerPluginArgument2"),
        compilerPluginClasspath = listOf(File("myCompilerPluginClasspath.jar").absoluteFile),
        freeCompilerArgs = listOf("myFreeCompilerArguments")
    )

    val simpleBinaryCoordinates = IdeaKpmBinaryCoordinatesImpl(
        group = "myGroup",
        module = "myModule",
        version = "myVersion",
        kotlinModuleName = "myKotlinModuleName",
        kotlinFragmentName = "myKotlinFragmentName"
    )

    val simpleUnresolvedBinaryDependency = IdeaKpmUnresolvedBinaryDependencyImpl(
        cause = "myCause",
        coordinates = simpleBinaryCoordinates
    )

    val simpleResolvedBinaryDependency = IdeaKpmResolvedBinaryDependencyImpl(
        coordinates = simpleBinaryCoordinates,
        binaryType = "myBinaryType",
        binaryFile = File("myBinaryFile.jar").absoluteFile
    )

    val simpleFragmentDependency = IdeaKpmFragmentDependencyImpl(
        type = IdeaKpmFragmentDependency.Type.Friend,
        coordinates = simpleFragmentCoordinates
    )

    val simpleSourceDirectory = IdeaKpmContentRootImpl(
        file = File("myFile").absoluteFile,
        type = "myType"
    )

    val simpleFragment = IdeaKpmFragmentImpl(
        coordinates = simpleFragmentCoordinates,
        platforms = setOf(simpleJvmPlatform),
        languageSettings = simpleLanguageSettings,
        dependencies = listOf(simpleUnresolvedBinaryDependency, simpleResolvedBinaryDependency, simpleFragmentDependency),
        contentRoots = listOf(simpleSourceDirectory),
        extras = emptyExtras()
    )

    val fragmentWithExtras = simpleFragment.copy(
        extras = extrasWithIntAndStrings
    )

    val simpleCompilationOutput = IdeaKpmCompilationOutputImpl(
        classesDirs = setOf(File("myClassesDir").absoluteFile),
        resourcesDir = File("myResourcesDir").absoluteFile
    )

    val simpleVariant = IdeaKpmVariantImpl(
        fragment = simpleFragment,
        platform = simpleJvmPlatform,
        variantAttributes = mapOf("key1" to "attribute1", "key2" to "attribute2"),
        compilationOutputs = simpleCompilationOutput
    )

    val variantWithExtras = simpleVariant.copy(
        fragment = fragmentWithExtras
    )

    val simpleModule = IdeaKpmModuleImpl(
        coordinates = simpleModuleCoordinates,
        fragments = listOf(simpleFragment, simpleVariant)
    )

    val simpleProject = IdeaKpmProjectImpl(
        gradlePluginVersion = "1.7.20",
        coreLibrariesVersion = "1.6.20",
        explicitApiModeCliOption = null,
        kotlinNativeHome = File("myKotlinNativeHome").absoluteFile,
        modules = listOf(simpleModule)
    )
}
