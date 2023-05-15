/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import kotlin.reflect.jvm.javaGetter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

class KotlinSourceSetConventionsTest {

    @Test
    fun `test - commonMain and commonTest`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        assertEquals("commonMain", kotlin.sourceSets.commonMain.get().name)
        assertEquals("commonTest", kotlin.sourceSets.commonTest.get().name)
    }

    @Test
    fun `test - accessing nativeMain when not created yet`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        assertNull(kotlin.sourceSets.findByName("nativeMain"))
        kotlin.sourceSets.nativeMain
        assertEquals("nativeMain", kotlin.sourceSets.findByName("nativeMain")?.name)
    }

    /**
     * Checks that all conventions defined will be referencing a SourceSet that exists
     * when targetHierarchy.default() is used.
     */
    @Test
    fun `test - all source set conventions exist in default hierarchy`() {
        val project = buildProjectWithMPP()
        project.androidLibrary { compileSdk = 33 }
        val kotlin = project.multiplatformExtension
        kotlin.targetHierarchy.default()
        kotlin.androidTarget()
        kotlin.jvm()
        kotlin.macosArm64()
        kotlin.iosArm64()
        kotlin.watchosArm64()
        kotlin.tvosArm64()
        kotlin.mingwX64()
        kotlin.linuxX64()
        kotlin.androidNativeArm64()
        kotlin.js(IR)

        val sourceFile = Class.forName("org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetConventionsKt")
        val commonMainConvention = NamedDomainObjectContainer<KotlinSourceSet>::commonMain.javaGetter!!
        val allSourceSetConventions = sourceFile.methods
            .filter { it.parameters.map { it.parameterizedType } == commonMainConvention.parameters.map { it.parameterizedType } }
            .filter { it.genericReturnType == commonMainConvention.genericReturnType }
            .apply { if (size <= 2) fail("Failed finding SourceSet conventions in $sourceFile") }

        allSourceSetConventions.forEach { convention ->
            val name = convention.name.removePrefix("get").decapitalizeAsciiOnly()
            if (name !in kotlin.sourceSets.names) fail("Convention '${name}' has no SourceSet created by defaultHierarchy()")
        }
    }
}