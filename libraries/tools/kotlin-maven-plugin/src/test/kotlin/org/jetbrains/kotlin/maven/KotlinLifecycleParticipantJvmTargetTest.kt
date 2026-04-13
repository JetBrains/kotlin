/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.apache.maven.model.Build
import org.apache.maven.model.Model
import org.apache.maven.model.Plugin
import org.apache.maven.model.PluginExecution
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import java.util.Properties
import kotlin.test.*

/**
 * Unit tests for [KotlinLifecycleParticipant] jvmTarget auto-alignment logic.
 */
class KotlinLifecycleParticipantJvmTargetTest {

    // -------------------------------------------------------------------------
    // normalizeToKotlinJvmTarget
    // -------------------------------------------------------------------------

    @Test
    fun `normalizeToKotlinJvmTarget - java 8 short form`() {
        assertEquals("1.8", KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("8"))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - java 8 long form`() {
        assertEquals("1.8", KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("1.8"))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - java 11`() {
        assertEquals("11", KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("11"))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - java 17`() {
        assertEquals("17", KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("17"))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - java 21`() {
        assertEquals("21", KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("21"))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - java 6 not supported`() {
        assertNull(KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("6"))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - java 1_6 not supported`() {
        assertNull(KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("1.6"))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - java 7 not supported`() {
        assertNull(KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("7"))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - java 1_7 not supported`() {
        assertNull(KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("1.7"))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - null input`() {
        assertNull(KotlinLifecycleParticipant.normalizeToKotlinJvmTarget(null))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - garbage input`() {
        assertNull(KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("abc"))
    }

    @Test
    fun `normalizeToKotlinJvmTarget - whitespace trimmed`() {
        assertEquals("17", KotlinLifecycleParticipant.normalizeToKotlinJvmTarget("  17  "))
    }

    // -------------------------------------------------------------------------
    // getPropertyOrPluginConfig
    // -------------------------------------------------------------------------

    @Test
    fun `getPropertyOrPluginConfig - returns config value when set`() {
        val props = Properties().apply { setProperty("maven.compiler.release", "17") }
        val config = compilerConfig("release" to "11")
        assertEquals("11", KotlinLifecycleParticipant.getPluginConfigOrProperty(
            config,
            "release",
            props,
            "maven.compiler.release"
        ))
    }

    @Test
    fun `getPropertyOrPluginConfig - falls back to plugin config when property absent`() {
        val props = Properties()
        val config = compilerConfig("release" to "17")
        assertEquals("17", KotlinLifecycleParticipant.getPluginConfigOrProperty(
            config,
            "release",
            props,
            "maven.compiler.release"
        ))
    }

    @Test
    fun `getPropertyOrPluginConfig - returns null when both absent`() {
        assertNull(KotlinLifecycleParticipant.getPluginConfigOrProperty(
            null,
            "release",
            Properties(),
            "maven.compiler.release"
        ))
    }

    @Test
    fun `getPropertyOrPluginConfig - blank property is skipped`() {
        val props = Properties().apply { setProperty("maven.compiler.release", "  ") }
        val config = compilerConfig("release" to "17")
        assertEquals("17", KotlinLifecycleParticipant.getPluginConfigOrProperty(
            config,
            "release",
            props,
            "maven.compiler.release"
        ))
    }

    @Test
    fun `getPropertyOrPluginConfig - null plugin config falls back gracefully`() {
        val props = Properties()
        assertNull(KotlinLifecycleParticipant.getPluginConfigOrProperty(
            null,
            "release",
            props,
            "maven.compiler.release"
        ))
    }

    // -------------------------------------------------------------------------
    // isJvmTargetAlreadyConfigured
    // -------------------------------------------------------------------------

    @Test
    fun `isJvmTargetAlreadyConfigured - false when nothing set`() {
        val project = projectWithProperties()
        val kotlinPlugin = kotlinPlugin()
        assertFalse(KotlinLifecycleParticipant.isJvmTargetOrJdkReleaseAlreadyConfigured(project, kotlinPlugin))
    }

    @Test
    fun `isJvmTargetAlreadyConfigured - true when project property set`() {
        val project = projectWithProperties("kotlin.compiler.jvmTarget" to "17")
        val kotlinPlugin = kotlinPlugin()
        assertTrue(KotlinLifecycleParticipant.isJvmTargetOrJdkReleaseAlreadyConfigured(project, kotlinPlugin))
    }

    @Test
    fun `isJvmTargetAlreadyConfigured - true when global plugin config has jvmTarget`() {
        val project = projectWithProperties()
        val kotlinPlugin = kotlinPlugin(globalJvmTarget = "17")
        assertTrue(KotlinLifecycleParticipant.isJvmTargetOrJdkReleaseAlreadyConfigured(project, kotlinPlugin))
    }

    @Test
    fun `isJvmTargetAlreadyConfigured - true when execution config has jvmTarget`() {
        val project = projectWithProperties()
        val kotlinPlugin = kotlinPlugin(executionJvmTarget = "17")
        assertTrue(KotlinLifecycleParticipant.isJvmTargetOrJdkReleaseAlreadyConfigured(project, kotlinPlugin))
    }

    @Test
    fun `isJvmTargetAlreadyConfigured - true when project property jdkRelease is set`() {
        val project = projectWithProperties("kotlin.compiler.jdkRelease" to "17")
        val kotlinPlugin = kotlinPlugin()
        assertTrue(KotlinLifecycleParticipant.isJvmTargetOrJdkReleaseAlreadyConfigured(project, kotlinPlugin))
    }

    @Test
    fun `isJvmTargetAlreadyConfigured - true when global plugin config has jdkRelease`() {
        val project = projectWithProperties()
        val kotlinPlugin = kotlinPlugin(globalJdkRelease = "17")
        assertTrue(KotlinLifecycleParticipant.isJvmTargetOrJdkReleaseAlreadyConfigured(project, kotlinPlugin))
    }

    @Test
    fun `isJvmTargetAlreadyConfigured - true when execution config has jdkRelease`() {
        val project = projectWithProperties()
        val kotlinPlugin = kotlinPlugin(executionJdkRelease = "17")
        assertTrue(KotlinLifecycleParticipant.isJvmTargetOrJdkReleaseAlreadyConfigured(project, kotlinPlugin))
    }

    // -------------------------------------------------------------------------
    // resolveJvmTarget - priority 1: maven.compiler.release
    // -------------------------------------------------------------------------

    @Test
    fun `resolveJvmTarget - uses maven_compiler_release property`() {
        val project = projectWithProperties("maven.compiler.release" to "17")
        val resolution = participant().resolveJvmTarget(project)
        assertNotNull(resolution)
        assertEquals("17", resolution.jvmTarget)
        assertTrue(resolution.useRelease)
        assertContains(resolution.derivedFrom, "maven.compiler.release")
    }

    @Test
    fun `resolveJvmTarget - uses maven-compiler-plugin release config`() {
        val compilerPlugin = compilerPluginWithConfig("release" to "17")
        val project = projectWithPlugins(compilerPlugin)
        val resolution = participant().resolveJvmTarget(project)
        assertNotNull(resolution)
        assertEquals("17", resolution.jvmTarget)
        assertTrue(resolution.useRelease)
    }

    @Test
    fun `resolveJvmTarget - release 8 normalizes to 1_8`() {
        val project = projectWithProperties("maven.compiler.release" to "8")
        val resolution = participant().resolveJvmTarget(project)
        assertNotNull(resolution)
        assertEquals("1.8", resolution.jvmTarget)
        assertTrue(resolution.useRelease)
    }

    @Test
    fun `resolveJvmTarget - compiler plugin release config wins over release property`() {
        val compilerPlugin = compilerPluginWithConfig("release" to "11")
        val project = projectWithPlugins(compilerPlugin, properties = mapOf("maven.compiler.release" to "17"))
        val resolution = participant().resolveJvmTarget(project)
        assertNotNull(resolution)
        assertEquals("11", resolution.jvmTarget)
    }

    @Test
    fun `resolveJvmTarget - unsupported release returns null`() {
        val project = projectWithProperties("maven.compiler.release" to "7")
        val resolution = participant().resolveJvmTarget(project)
        // Unsupported version: entire resolution aborted (return null, not falling through)
        assertNull(resolution)
    }

    // -------------------------------------------------------------------------
    // resolveJvmTarget - priority 2: maven.compiler.target
    // -------------------------------------------------------------------------

    @Test
    fun `resolveJvmTarget - uses maven_compiler_target property`() {
        val project = projectWithProperties("maven.compiler.target" to "11")
        val resolution = participant().resolveJvmTarget(project)
        assertNotNull(resolution)
        assertEquals("11", resolution.jvmTarget)
        assertFalse(resolution.useRelease)
        assertContains(resolution.derivedFrom, "maven.compiler.target")
    }

    @Test
    fun `resolveJvmTarget - target do not set useRelease`() {
        val project = projectWithProperties("maven.compiler.target" to "17")
        val resolution = participant().resolveJvmTarget(project)
        assertNotNull(resolution)
        assertFalse(resolution.useRelease)
    }

    @Test
    fun `resolveJvmTarget - uses maven-compiler-plugin target config`() {
        val compilerPlugin = compilerPluginWithConfig("target" to "17")
        val project = projectWithPlugins(compilerPlugin)
        val resolution = participant().resolveJvmTarget(project)
        assertNotNull(resolution)
        assertEquals("17", resolution.jvmTarget)
        assertFalse(resolution.useRelease)
    }

    @Test
    fun `resolveJvmTarget - release takes priority over target`() {
        val project = projectWithProperties(
            "maven.compiler.release" to "17",
            "maven.compiler.target" to "11"
        )
        val resolution = participant().resolveJvmTarget(project)
        assertNotNull(resolution)
        assertEquals("17", resolution.jvmTarget)
        assertTrue(resolution.useRelease)
    }

    @Test
    fun `resolveJvmTarget - target 1_8 normalizes to 1_8`() {
        val project = projectWithProperties("maven.compiler.target" to "1.8")
        val resolution = participant().resolveJvmTarget(project)
        assertNotNull(resolution)
        assertEquals("1.8", resolution.jvmTarget)
    }

    @Test
    fun `resolveJvmTarget - returns null when no compiler config`() {
        val project = projectWithPlugins()
        assertNull(participant().resolveJvmTarget(project))
    }

    // -------------------------------------------------------------------------
    // applyJvmTargetToPlugin via full configureJvmTarget flow
    // -------------------------------------------------------------------------

    @Test
    fun `full flow - sets jvmTarget as project property`() {
        val project = projectWithProperties("maven.compiler.release" to "17")
        val kotlinPlugin = kotlinPlugin()
        participant().configureJvmTargetForTest(project, kotlinPlugin)

        assertEquals("17", project.properties.getProperty("kotlin.compiler.jvmTarget"))
    }

    @Test
    fun `full flow - sets jdkRelease project property when using release`() {
        val project = projectWithProperties("maven.compiler.release" to "17")
        val kotlinPlugin = kotlinPlugin()
        participant().configureJvmTargetForTest(project, kotlinPlugin)

        assertEquals("17", project.properties.getProperty("kotlin.compiler.jdkRelease"))
    }

    @Test
    fun `full flow - does NOT set jdkRelease when using target`() {
        val project = projectWithProperties("maven.compiler.target" to "17")
        val kotlinPlugin = kotlinPlugin()
        participant().configureJvmTargetForTest(project, kotlinPlugin)

        assertEquals("17", project.properties.getProperty("kotlin.compiler.jvmTarget"))
        assertNull(project.properties.getProperty("kotlin.compiler.jdkRelease"))
    }

    @Test
    fun `full flow - skips when jvmTarget already in project property`() {
        val project = projectWithProperties("kotlin.compiler.jvmTarget" to "11", "maven.compiler.release" to "17")
        val kotlinPlugin = kotlinPlugin()
        participant().configureJvmTargetForTest(project, kotlinPlugin)

        // The pre-existing property must not be overwritten
        assertEquals("11", project.properties.getProperty("kotlin.compiler.jvmTarget"))
        assertNull(project.properties.getProperty("kotlin.compiler.jdkRelease"))
    }

    @Test
    fun `full flow - skips when jvmTarget already in global plugin config`() {
        val project = projectWithProperties("maven.compiler.release" to "17")
        val kotlinPlugin = kotlinPlugin(globalJvmTarget = "21")
        participant().configureJvmTargetForTest(project, kotlinPlugin)

        // Smart defaults must not override an explicit user setting
        assertNull(project.properties.getProperty("kotlin.compiler.jvmTarget"))
    }

    @Test
    fun `full flow - skips when jvmTarget in execution config`() {
        val project = projectWithProperties("maven.compiler.release" to "17")
        val kotlinPlugin = kotlinPlugin(executionJvmTarget = "21")
        participant().configureJvmTargetForTest(project, kotlinPlugin)

        assertNull(project.properties.getProperty("kotlin.compiler.jvmTarget"))
    }

    @Test
    fun `full flow - skips when jdkRelease already in project property`() {
        val project = projectWithProperties("kotlin.compiler.jdkRelease" to "11", "maven.compiler.release" to "17")
        val kotlinPlugin = kotlinPlugin()
        participant().configureJvmTargetForTest(project, kotlinPlugin)

        assertEquals("11", project.properties.getProperty("kotlin.compiler.jdkRelease"))
        assertNull(project.properties.getProperty("kotlin.compiler.jvmTarget"))
    }

    @Test
    fun `full flow - skips when jdkRelease already in global plugin config`() {
        val project = projectWithProperties("maven.compiler.release" to "17")
        val kotlinPlugin = kotlinPlugin(globalJdkRelease = "21")
        participant().configureJvmTargetForTest(project, kotlinPlugin)

        assertNull(project.properties.getProperty("kotlin.compiler.jvmTarget"))
    }

    @Test
    fun `full flow - skips when jdkRelease in execution config`() {
        val project = projectWithProperties("maven.compiler.release" to "17")
        val kotlinPlugin = kotlinPlugin(executionJdkRelease = "21")
        participant().configureJvmTargetForTest(project, kotlinPlugin)

        assertNull(project.properties.getProperty("kotlin.compiler.jvmTarget"))
    }

    @Test
    fun `full flow - jdkRelease property value matches resolved jvmTarget`() {
        val project = projectWithProperties("maven.compiler.release" to "1.8")
        val kotlinPlugin = kotlinPlugin()
        participant().configureJvmTargetForTest(project, kotlinPlugin)

        // Java 1.8 normalizes to Kotlin "1.8"
        assertEquals("1.8", project.properties.getProperty("kotlin.compiler.jvmTarget"))
        assertEquals("1.8", project.properties.getProperty("kotlin.compiler.jdkRelease"))
    }

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private fun participant(): KotlinLifecycleParticipant = KotlinLifecycleParticipant()

    /** Exposes the package-private configureJvmTarget method via reflection for white-box testing. */
    private fun KotlinLifecycleParticipant.configureJvmTargetForTest(project: MavenProject, plugin: Plugin) {
        val method = KotlinLifecycleParticipant::class.java.getDeclaredMethod(
            "configureJvmTarget",
            MavenProject::class.java,
            Plugin::class.java
        )
        method.isAccessible = true
        method.invoke(this, project, plugin)
    }

    private fun projectWithProperties(vararg pairs: Pair<String, String>): MavenProject {
        val model = Model()
        pairs.forEach { (k, v) -> model.properties[k] = v }
        return MavenProject(model)
    }

    private fun projectWithPlugins(vararg plugins: Plugin, properties: Map<String, String> = emptyMap()): MavenProject {
        val model = Model()
        properties.forEach { (k, v) -> model.properties[k] = v }
        val build = Build()
        plugins.forEach { build.addPlugin(it) }
        model.build = build
        return MavenProject(model)
    }

    private fun compilerConfig(vararg entries: Pair<String, String>): Xpp3Dom {
        val config = Xpp3Dom("configuration")
        entries.forEach { (key, value) ->
            val child = Xpp3Dom(key)
            child.value = value
            config.addChild(child)
        }
        return config
    }

    private fun compilerPluginWithConfig(vararg entries: Pair<String, String>): Plugin {
        return Plugin().apply {
            groupId = "org.apache.maven.plugins"
            artifactId = "maven-compiler-plugin"
            configuration = compilerConfig(*entries)
        }
    }

    private fun kotlinPlugin(
        globalJvmTarget: String? = null,
        executionJvmTarget: String? = null,
        globalJdkRelease: String? = null,
        executionJdkRelease: String? = null
    ): Plugin {
        val plugin = Plugin().apply {
            groupId = "org.jetbrains.kotlin"
            artifactId = "kotlin-maven-plugin"
        }
        if (globalJvmTarget != null || globalJdkRelease != null) {
            val config = Xpp3Dom("configuration")
            if (globalJvmTarget != null) config.addChild(Xpp3Dom("jvmTarget").also { it.value = globalJvmTarget })
            if (globalJdkRelease != null) config.addChild(Xpp3Dom("jdkRelease").also { it.value = globalJdkRelease })
            plugin.configuration = config
        }
        if (executionJvmTarget != null || executionJdkRelease != null) {
            val execConfig = Xpp3Dom("configuration")
            if (executionJvmTarget != null) execConfig.addChild(Xpp3Dom("jvmTarget").also { it.value = executionJvmTarget })
            if (executionJdkRelease != null) execConfig.addChild(Xpp3Dom("jdkRelease").also { it.value = executionJdkRelease })
            val execution = PluginExecution().apply {
                id = "default-compile"
                configuration = execConfig
            }
            plugin.addExecution(execution)
        }
        return plugin
    }
}
