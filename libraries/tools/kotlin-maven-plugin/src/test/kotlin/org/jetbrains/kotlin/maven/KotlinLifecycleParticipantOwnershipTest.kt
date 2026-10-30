/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.apache.maven.AbstractMavenLifecycleParticipant
import org.apache.maven.execution.DefaultMavenExecutionRequest
import org.apache.maven.execution.DefaultMavenExecutionResult
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Build
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.Plugin
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.ClassRealm
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.*

class KotlinLifecycleParticipantOwnershipTest {
    @TempDir
    lateinit var tempDir: Path

    private val world = ClassWorld()

    @AfterTest
    fun closeRealms() {
        world.realms.forEach { it.close() }
    }

    @Test
    fun `participants configure only their modules including modules sharing a realm`() {
        checkOwnership(reverseParticipants = false)
    }

    @Test
    fun `ownership does not depend on participant invocation order`() {
        checkOwnership(reverseParticipants = true)
    }

    @Test
    fun `an existing stdlib does not prevent configuring the other defaults`() {
        val realm = extensionRealm("kotlin")
        val project = project("existing-stdlib", realm)
        project.dependencies.add(Dependency().apply {
            groupId = "org.jetbrains.kotlin"
            artifactId = "kotlin-stdlib"
            version = "explicit-version"
        })

        participant(realm).afterProjectsRead(session(project))

        assertConfigured(project, stdlibVersion = "explicit-version")
    }

    @Test
    fun `missing project realm leaves the project unchanged`() {
        val project = project("missing-realm", null)

        KotlinLifecycleParticipant().afterProjectsRead(session(project))

        assertUnconfigured(project)
    }

    @Test
    fun `project realm without extension imports leaves the project unchanged`() {
        val project = project("empty-realm", null)
        project.classRealm = world.newRealm("empty")

        KotlinLifecycleParticipant().afterProjectsRead(session(project))

        assertUnconfigured(project)
    }

    @Test
    fun `disabled defaults and non-extension plugins do not require a realm`() {
        val disabled = project("disabled", null).apply {
            properties.setProperty("kotlin.smart.defaults.enabled", "false")
        }
        val nonExtension = project("non-extension", null).apply {
            buildPlugins.single().isExtensions = false
        }

        KotlinLifecycleParticipant().afterProjectsRead(session(disabled, nonExtension))

        assertUnconfigured(disabled)
        assertUnconfigured(nonExtension)
    }

    private fun checkOwnership(reverseParticipants: Boolean) {
        val plainRealm = extensionRealm("kotlin-plain")
        val allOpenRealm = extensionRealm("kotlin-allopen")
        val first = project("first", plainRealm)
        val second = project("second", allOpenRealm)
        val third = project("third", plainRealm)
        val session = session(first, second, third)
        val plain = participant(plainRealm)
        val allOpen = participant(allOpenRealm)

        val firstParticipant = if (reverseParticipants) allOpen else plain
        firstParticipant.afterProjectsRead(session)
        if (reverseParticipants) {
            assertUnconfigured(first)
            assertConfigured(second)
            assertUnconfigured(third)
        } else {
            assertConfigured(first)
            assertUnconfigured(second)
            assertConfigured(third)
        }

        val secondParticipant = if (reverseParticipants) plain else allOpen
        secondParticipant.afterProjectsRead(session)
        listOf(first, second, third).forEach { assertConfigured(it) }
    }

    private fun extensionRealm(id: String): ClassRealm =
        world.newRealm(id, null).apply {
            importFrom(javaClass.classLoader, "org.apache.maven")
            importFrom(javaClass.classLoader, "org.codehaus.plexus")
            addURL(KotlinLifecycleParticipant::class.java.protectionDomain.codeSource.location)
        }

    private fun participant(realm: ClassRealm): AbstractMavenLifecycleParticipant {
        val instance = realm.loadClass(KotlinLifecycleParticipant::class.java.name)
            .getDeclaredConstructor().newInstance() as AbstractMavenLifecycleParticipant
        assertSame(realm, instance.javaClass.classLoader)
        return instance
    }

    private fun project(id: String, extensionRealm: ClassRealm?): MavenProject {
        val baseDir = tempDir.resolve(id).toFile()
        baseDir.resolve("src/main/kotlin").mkdirs()
        baseDir.resolve("src/test/kotlin").mkdirs()
        val model = Model().apply {
            groupId = "test"
            artifactId = id
            version = "1"
            build = Build().apply {
                addPlugin(Plugin().apply {
                    groupId = "org.jetbrains.kotlin"
                    artifactId = "kotlin-maven-plugin"
                    version = "same-version"
                    isExtensions = true
                })
            }
            properties.setProperty("maven.compiler.release", "17")
        }
        return MavenProject(model).apply {
            file = baseDir.resolve("pom.xml")
            if (extensionRealm != null) {
                classRealm = world.newRealm("project-$id").apply {
                    // Maven uses realm IDs for component-only imports.
                    importFrom(extensionRealm, extensionRealm.id)
                    val unrelated = world.newRealm("unrelated-$id")
                    importFrom(unrelated, unrelated.id)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun session(vararg projects: MavenProject): MavenSession =
        MavenSession(null, DefaultMavenExecutionRequest(), DefaultMavenExecutionResult(), projects.toList())

    private fun assertConfigured(project: MavenProject, stdlibVersion: String = "same-version") {
        assertEquals(listOf(project.basedir.resolve("src/main/kotlin").absolutePath), project.compileSourceRoots)
        assertEquals(listOf(project.basedir.resolve("src/test/kotlin").absolutePath), project.testCompileSourceRoots)
        val stdlib = project.dependencies.single()
        assertEquals("org.jetbrains.kotlin", stdlib.groupId)
        assertEquals("kotlin-stdlib", stdlib.artifactId)
        assertEquals(stdlibVersion, stdlib.version)
        assertEquals("17", project.properties.getProperty("kotlin.compiler.jvmTarget"))
        assertEquals("17", project.properties.getProperty("kotlin.compiler.jdkRelease"))
    }

    private fun assertUnconfigured(project: MavenProject) {
        assertTrue(project.compileSourceRoots.isEmpty())
        assertTrue(project.testCompileSourceRoots.isEmpty())
        assertTrue(project.dependencies.isEmpty())
        assertNull(project.properties.getProperty("kotlin.compiler.jvmTarget"))
        assertNull(project.properties.getProperty("kotlin.compiler.jdkRelease"))
    }
}
