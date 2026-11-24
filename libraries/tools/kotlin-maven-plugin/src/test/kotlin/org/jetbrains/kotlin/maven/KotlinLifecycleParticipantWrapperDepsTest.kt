/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.apache.maven.model.Dependency
import org.apache.maven.model.Plugin
import kotlin.test.*

/**
 * Unit tests for redundant compiler plugin wrapper dependency detection in [KotlinLifecycleParticipant].
 */
class KotlinLifecycleParticipantWrapperDepsTest {

    private fun dependency(groupId: String, artifactId: String): Dependency =
        Dependency().apply {
            setGroupId(groupId)
            setArtifactId(artifactId)
            setVersion("2.0.0")
        }

    private fun kotlinMavenPlugin(vararg dependencies: Dependency): Plugin =
        Plugin().apply {
            setGroupId("org.jetbrains.kotlin")
            setArtifactId("kotlin-maven-plugin")
            dependencies.forEach { addDependency(it) }
        }

    @Test
    fun `wrapper artifact ids are exactly the seven published wrappers`() {
        assertEquals(
            setOf(
                "kotlin-maven-allopen",
                "kotlin-maven-noarg",
                "kotlin-maven-sam-with-receiver",
                "kotlin-maven-lombok",
                "kotlin-maven-power-assert",
                "kotlin-maven-serialization",
                "kotlin-maven-dataframe",
            ),
            CompilerPluginWrapperArtifacts.WRAPPER_ARTIFACT_IDS
        )
    }

    @Test
    fun `findRedundantWrapperDependencies - returns declared wrapper dependencies`() {
        val redundant = KotlinLifecycleParticipant.findRedundantWrapperDependencies(
            kotlinMavenPlugin(
                dependency("org.jetbrains.kotlin", "kotlin-maven-allopen"),
                dependency("junit", "junit"),
                dependency("org.jetbrains.kotlin", "kotlin-stdlib"),
                dependency("org.jetbrains.kotlin", "kotlin-maven-dataframe"),
            )
        )

        assertEquals(
            listOf("kotlin-maven-allopen", "kotlin-maven-dataframe"),
            redundant.map { it.artifactId }
        )
    }

    @Test
    fun `findRedundantWrapperDependencies - ignores wrapper artifact ids with other group ids`() {
        val redundant = KotlinLifecycleParticipant.findRedundantWrapperDependencies(
            kotlinMavenPlugin(
                dependency("com.example", "kotlin-maven-allopen"),
                dependency("org.jetbrains.kotlin", "kotlin-maven-plugin-api"),
            )
        )

        assertTrue(redundant.isEmpty())
    }

    @Test
    fun `findRedundantWrapperDependencies - no dependencies declared`() {
        assertTrue(KotlinLifecycleParticipant.findRedundantWrapperDependencies(kotlinMavenPlugin()).isEmpty())
    }
}
