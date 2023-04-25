/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.toMap
import java.util.*
import kotlin.test.*

class ConfigurationsTest : MultiplatformExtensionTest() {

    @Test
    fun `source set dependencies dsl test`() {
        val lib = buildProjectWithMPP(projectBuilder = { withName("lib"); withParent(project) }) {
            kotlin {
                jvm()
                linuxX64()
            }

            configurations.create("outputConfiguration") {
                it.isCanBeConsumed = true
                it.isCanBeResolved = false
            }
        }

        kotlin.jvm()
        kotlin.linuxX64()

        kotlin.sourceSets.getByName("commonMain").dependencies {
            api("junit:junit:4.13.2") { setTransitive(false) }
            api(kotlin("reflect")) { setTransitive(false) }
            api(kotlin("reflect", "1.3.0"))
            implementation(kotlin("reflect", "1.2.71"))
            compileOnly(kotlin("reflect", "1.2.70"))
            runtimeOnly(kotlin("reflect", "1.2.60"))
            api(project(path = ":lib", configuration = "outputConfiguration"))
        }

        project.evaluate()

        val commonMainApi = project.configurations.getByName("commonMainApi")
        val commonMainImplementation = project.configurations.getByName("commonMainImplementation")
        val commonMainCompileOnly = project.configurations.getByName("commonMainCompileOnly")
        val commonMainRuntimeOnly = project.configurations.getByName("commonMainRuntimeOnly")

        fun Configuration.assertHasDependency(criterionName: String, criterion: Dependency.() -> Boolean) {
            val allDependenciesString = allDependencies.joinToString("\n")
            val message = "Configuration expected to have dependency: ${criterionName}.\n" +
                    "But it has only dependencies: \n$allDependenciesString"
            assertTrue(message) { allDependencies.any(criterion) }
        }

        commonMainApi.assertHasDependency("non-transitive string notation of junit:junit:4.13.2") {
            this is ModuleDependency &&
            group == "junit" &&
            name == "junit" &&
            version == "4.13.2" &&
            !isTransitive
        }

        commonMainApi.assertHasDependency("non-transitive dependency notation of kotlin-reflect without version") {
            this is ModuleDependency &&
            group == "org.jetbrains.kotlin" &&
            name == "kotlin-reflect" &&
            version == null &&
            !isTransitive
        }

        commonMainApi.assertHasDependency("dependency notation of kotlin-reflect:1.3.0") {
            this is ModuleDependency &&
            group == "org.jetbrains.kotlin" &&
            name == "kotlin-reflect" &&
            version == "1.3.0"
        }

        commonMainApi.assertHasDependency("project notation of :lib:outputConfiguration") {
            this is ProjectDependency &&
            dependencyProject == lib &&
            targetConfiguration == "outputConfiguration"
        }

        commonMainImplementation.assertHasDependency("dependency notation of kotlin-reflect:1.2.71") {
            this is ModuleDependency &&
            group == "org.jetbrains.kotlin" &&
            name == "kotlin-reflect" &&
            version == "1.2.71"
        }

        commonMainCompileOnly.assertHasDependency("dependency notation of kotlin-reflect:1.2.70") {
            this is ModuleDependency &&
            group == "org.jetbrains.kotlin" &&
            name == "kotlin-reflect" &&
            version == "1.2.70"
        }

        commonMainRuntimeOnly.assertHasDependency("dependency notation of kotlin-reflect:1.2.60") {
            this is ModuleDependency &&
            group == "org.jetbrains.kotlin" &&
            name == "kotlin-reflect" &&
            version == "1.2.60"
        }
    }

    @Test
    fun `consumable configurations except sourcesElements with platform target are marked with Category LIBRARY`() {
        kotlin.linuxX64()
        kotlin.iosX64()
        kotlin.iosArm64()
        kotlin.jvm()
        kotlin.js()

        val nativeMain = kotlin.sourceSets.create("nativeMain")
        kotlin.targets.withType(KotlinNativeTarget::class.java).all { target ->
            target.compilations.getByName("main").defaultSourceSet.dependsOn(nativeMain)
        }

        project.evaluate()

        project.configurations
            .filter { configuration ->
                configuration.attributes.contains(KotlinPlatformType.attribute) ||
                        configuration.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name in KotlinUsages.values
            }
            .filterNot { configuration -> configuration.name.contains("SourcesElements") }
            .forEach { configuration ->
                val category = configuration.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)
                assertNotNull(category, "Expected configuration ${configuration.name} to provide 'Category' attribute")
                assertEquals(Category.LIBRARY, category.name, "Expected configuration $configuration to be 'LIBRARY' Category")
            }
    }

    @Test
    fun `don't publish wasm targets with KotlinJsCompilerAttribute attribute`() {
        with(kotlin) {
            val jsAttribute = Attribute.of(String::class.java)
            js("nodeJs", KotlinJsCompilerType.IR) { attributes { attribute(jsAttribute, "nodeJs") } }
            js("browser", KotlinJsCompilerType.IR) { attributes { attribute(jsAttribute, "browser") } }
            @OptIn(ExperimentalWasmDsl::class)
            wasm()

            val allJs = sourceSets.create("allJs")
            targets.getByName("nodeJs").compilations.getByName("main").defaultSourceSet.dependsOn(allJs)
            targets.getByName("browser").compilations.getByName("main").defaultSourceSet.dependsOn(allJs)
        }

        project.evaluate()

        val targetSpecificConfigurationsToCheck = listOf(
            "ApiElements",
            "RuntimeElements",

            "MainApiDependenciesMetadata",
            "MainCompileOnlyDependenciesMetadata",
            "MainImplementationDependenciesMetadata",

            "TestApiDependenciesMetadata",
            "TestCompileOnlyDependenciesMetadata",
            "TestImplementationDependenciesMetadata",
        )

        // WASM
        val actualWasmConfigurations = targetSpecificConfigurationsToCheck
            .map { project.configurations.getByName("wasm$it") }
            .filter { it.attributes.contains(KotlinJsCompilerAttribute.jsCompilerAttribute) }

        assertEquals(
            emptyList(),
            actualWasmConfigurations,
            "All WASM configurations should not contain KotlinJsCompilerAttribute"
        )

        val commonSourceSetsConfigurationsToCheck = listOf(
            "ApiDependenciesMetadata",
            "CompileOnlyDependenciesMetadata",
            "ImplementationDependenciesMetadata",
        )

        // allJs
        val expectedAllJsConfigurations = commonSourceSetsConfigurationsToCheck
            .map { project.configurations.getByName("allJs$it") }

        val actualAllJsConfigurations = expectedAllJsConfigurations
            .filter { it.attributes.contains(KotlinJsCompilerAttribute.jsCompilerAttribute) }

        assertEquals(
            expectedAllJsConfigurations,
            actualAllJsConfigurations,
            "JS-only configurations should contain KotlinJsCompilerAttribute"
        )


        // commonMain
        val actualCommonMainConfigurations = commonSourceSetsConfigurationsToCheck
            .map { project.configurations.getByName("commonMain$it") }
            .filter { it.attributes.contains(KotlinJsCompilerAttribute.jsCompilerAttribute) }

        assertEquals(
            emptyList(),
            actualCommonMainConfigurations,
            "commonMain configurations should not contain KotlinJsCompilerAttribute"
        )

    }

    @Test
    fun `test js IR compilation dependencies`() {
        val project = buildProjectWithMPP {
            kotlin {
                @Suppress("DEPRECATION")
                js(BOTH)
                targets.withType<KotlinJsTarget> {
                    irTarget!!.compilations.getByName("main").dependencies {
                        api("test:compilation-dependency")
                    }
                }

                sourceSets.getByName("jsMain").apply {
                    dependencies {
                        api("test:source-set-dependency")
                    }
                }
            }
        }

        project.evaluate()

        with(project) {
            assertContainsDependencies("jsCompilationApi", "test:compilation-dependency", "test:source-set-dependency")
            assertContainsDependencies("jsMainApi", "test:source-set-dependency")
            assertNotContainsDependencies("jsMainApi", "test:compilation-dependency")
        }
    }

    @Test
    fun `test compilation and source set configurations don't clash`() {
        val project = buildProjectWithMPP {
            androidLibrary {
                compileSdk = 30
            }

            kotlin {
                jvm()
                @Suppress("DEPRECATION")
                js(BOTH)
                linuxX64("linux")
                android()
            }
        }

        project.evaluate()

        project.kotlinExtension.targets.flatMap { it.compilations }.forEach { compilation ->
            val compilationSourceSets = compilation.allKotlinSourceSets
            val compilationConfigurationNames = compilation.relatedConfigurationNames
            val sourceSetConfigurationNames = compilationSourceSets.flatMapTo(mutableSetOf()) { it.relatedConfigurationNames }

            assert(compilationConfigurationNames.none { it in sourceSetConfigurationNames }) {
                """A name clash between source set and compilation configurations detected for the following configurations:
                    |${compilationConfigurationNames.filter { it in sourceSetConfigurationNames }.joinToString()}
                """.trimMargin()
            }
        }
    }

    @Test
    fun `test scoped sourceSet's configurations don't extend other configurations`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                @Suppress("DEPRECATION")
                js(BOTH)
                linuxX64("linux")
            }
        }

        project.evaluate()

        for (sourceSet in project.kotlinExtension.sourceSets) {
            val configurationNames = listOf(
                sourceSet.implementationConfigurationName,
                sourceSet.apiConfigurationName,
                sourceSet.compileOnlyConfigurationName,
                sourceSet.runtimeOnlyConfigurationName,
            )

            for (name in configurationNames) {
                val extendsFrom = project.configurations.getByName(name).extendsFrom
                assert(extendsFrom.isEmpty()) {
                    "Configuration $name is not expected to be extending anything, but it extends: ${
                        extendsFrom.joinToString(
                            prefix = "[",
                            postfix = "]"
                        ) { it.name }
                    }"
                }
            }
        }
    }

    class TestDisambiguationAttributePropagation {
        private val disambiguationAttribute = org.gradle.api.attributes.Attribute.of("disambiguationAttribute", String::class.java)

        private val mppProject
            get() = buildProjectWithMPP {
                kotlin {
                    jvm("plainJvm") {
                        attributes { attribute(disambiguationAttribute, "plainJvm") }
                    }

                    jvm("jvmWithJava") {
                        withJava()
                        attributes { attribute(disambiguationAttribute, "jvmWithJava") }
                    }
                }
            }

        private val javaProject
            get() = buildProject {
                project.plugins.apply("java-library")
            }

        //NB: There is no "api" configuration registered by Java Plugin
        private val javaConfigurations = listOf(
            "compileClasspath",
            "runtimeClasspath",
            "implementation",
            "compileOnly",
            "runtimeOnly"
        )

        @Test
        fun `test that jvm target attributes are propagated to java configurations`() {
            val kotlinJvmConfigurations = listOf(
                "jvmWithJavaCompileClasspath",
                "jvmWithJavaRuntimeClasspath",
                "jvmWithJavaCompilationApi",
                "jvmWithJavaCompilationImplementation",
                "jvmWithJavaCompilationCompileOnly",
                "jvmWithJavaCompilationRuntimeOnly",
            )

            val outgoingConfigurations = listOf(
                "jvmWithJavaApiElements",
                "jvmWithJavaRuntimeElements",
                "jvmWithJavaSourcesElements",
            )

            val testJavaConfigurations = listOf(
                "testCompileClasspath",
                "testCompileOnly",
                "testImplementation",
                "testRuntimeClasspath",
                "testRuntimeOnly"
            )

            val jvmWithJavaTestConfigurations = listOf(
                "jvmWithJavaTestCompileClasspath",
                "jvmWithJavaTestRuntimeClasspath",
                "jvmWithJavaTestCompilationApi",
                "jvmWithJavaTestCompilationCompileOnly",
                "jvmWithJavaTestCompilationImplementation",
                "jvmWithJavaTestCompilationRuntimeOnly"
            )

            val expectedConfigurationsWithDisambiguationAttribute = javaConfigurations +
                    kotlinJvmConfigurations +
                    outgoingConfigurations +
                    testJavaConfigurations +
                    jvmWithJavaTestConfigurations

            with(mppProject.evaluate()) {
                val actualConfigurationsWithDisambiguationAttribute = configurations
                    .filter { it.attributes.getAttribute(disambiguationAttribute) == "jvmWithJava" }
                    .map { it.name }

                assertEquals(
                    expectedConfigurationsWithDisambiguationAttribute.sorted(),
                    actualConfigurationsWithDisambiguationAttribute.sorted()
                )
            }
        }

        @Test
        fun `test that no new attributes are added to java configurations`() {
            val evaluatedJavaProject = javaProject.evaluate()
            val evaluatedMppProject = mppProject.evaluate()

            fun AttributeContainer.toStringMap(): Map<String, String> =
                keySet().associate { it.name to getAttribute(it).toString() }

            for (configurationName in javaConfigurations) {
                val expectedAttributes = evaluatedJavaProject
                    .configurations
                    .getByName(configurationName)
                    .attributes.toStringMap()

                val actualAttributes = evaluatedMppProject
                    .configurations
                    .getByName(configurationName)
                    .attributes.toStringMap()

                assertEquals(
                    expectedAttributes,
                    actualAttributes - disambiguationAttribute.name
                )
            }
        }
    }

    @Test
    fun `test platform notation for BOM is consumable in dependencies`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                sourceSets.getByName("jvmMain").apply {
                    dependencies {
                        api(platform("test:platform-dependency:1.0.0"))
                    }
                }
            }
        }

        project.evaluate()

        project.assertContainsDependencies("jvmMainApi", project.dependencies.platform("test:platform-dependency:1.0.0"))
    }


    @Test
    fun `test enforcedPlatform notation for BOM is consumable in dependencies`() {
        val project = buildProjectWithMPP {
            kotlin {
                js("browser") {
                    browser {
                        binaries.executable()
                    }
                }
                sourceSets.getByName("browserMain").apply {
                    dependencies {
                        implementation(enforcedPlatform("test:enforced-platform-dependency"))
                    }
                }
            }
        }

        project.evaluate()

        project.assertContainsDependencies(
            "browserMainImplementation",
            project.dependencies.enforcedPlatform("test:enforced-platform-dependency")
        )
    }

    /**
     * This tests verifies only turkish letters 'İ' and 'ı' because only with turkish locale ASCII letters 'i' and 'I' are
     * capitalised/decapitalised to non-ascii letters.
     * It was discovered using code that iterates over all ascii chars and available JVM locales
     * and checks their capitalisation/decapitalisation behavior.
     */
    @Test
    fun `gradle entities should have correct names when default locale is turkish`() {
        fun withLocale(locale: Locale, code: () -> Unit) {
            val currentLocal = Locale.getDefault()
            try {
                Locale.setDefault(locale)
                code()
            } finally {
                Locale.setDefault(currentLocal)
            }
        }

        withLocale(Locale("tr", "TR")) {
            val project = buildProjectWithMPP {
                kotlin {
                    jvm()
                    js().nodejs()
                    ios()
                }
            }
            project.evaluate()

            val gradleEntityNames: List<String> = with(project) {
                listOf(
                    tasks.names,
                    configurations.names,
                    components.names,
                    extensions.asMap.keys,
                    kotlin.sourceSets.names,
                    kotlin.targets.names,
                    kotlin.presets.names,
                ).flatten()
            }

            val entityNamesWithTurkishI = gradleEntityNames.filter { it.contains('İ') || it.contains('ı') }
            assertTrue(
                entityNamesWithTurkishI.isEmpty(),
                "Following entities should not have turkish 'İ' or 'ı' in their names:\n" +
                        entityNamesWithTurkishI.joinToString("\n")
            )
        }
    }

    // See KT-55697
    @Test
    fun testCompileOnlyDependenciesDontGetToAndroidTests() {
        val project = buildProject {
            applyKotlinAndroidPlugin()
            androidLibrary {
                compileSdk = 31
            }
        }

        project.dependencies {
            add("compileOnly", "org:example:1.0")
        }

        project.evaluate()

        fun isTestDependencyPresent(configName: String): Boolean =
            project.configurations.getByName(configName).incoming.dependencies.any { it.name == "example" }

        assertTrue(isTestDependencyPresent("debugCompileClasspath"))
        assertTrue(isTestDependencyPresent("releaseCompileClasspath"))

        assertFalse(isTestDependencyPresent("debugRuntimeClasspath"))
        assertFalse(isTestDependencyPresent("debugAndroidTestCompileClasspath"))
        assertFalse(isTestDependencyPresent("debugAndroidTestRuntimeClasspath"))
        assertFalse(isTestDependencyPresent("debugUnitTestCompileClasspath"))
        assertFalse(isTestDependencyPresent("debugUnitTestRuntimeClasspath"))
        assertFalse(isTestDependencyPresent("releaseRuntimeClasspath"))
        assertFalse(isTestDependencyPresent("releaseUnitTestCompileClasspath"))
        assertFalse(isTestDependencyPresent("releaseUnitTestRuntimeClasspath"))
    }

    // See KT-55751
    @Test
    fun `consumable configurations should have unique attribute set`() {
        val project = buildProjectWithMPP {
            plugins.apply("maven-publish")

            val distinguishingAttribute = Attribute.of(String::class.java)
            kotlin {
                jvm { attributes { attribute(distinguishingAttribute, "jvm") } }
                jvm("jvm2") { attributes { attribute(distinguishingAttribute, "jvm2") } }

                macosX64 {
                    binaries.framework("main", listOf(NativeBuildType.DEBUG))
                }

                linuxX64("linuxA") { attributes { attribute(distinguishingAttribute, "linuxA") } }
                linuxX64("linuxB") { attributes { attribute(distinguishingAttribute, "linuxB") } }

                targets.filterIsInstance<KotlinNativeTarget>().forEach {
                    it.binaries {
                        sharedLib("main", listOf(NativeBuildType.DEBUG))
                        staticLib("main", listOf(NativeBuildType.DEBUG))
                    }
                }
            }
        }

        project.evaluate()

        val duplicatedConsumableConfigurations = project.configurations
            .filter { it.isCanBeConsumed }
            .filterNot { it.attributes.isEmpty }
            .groupBy { it.attributes.toMap() }
            .values
            .filter { it.size > 1 }

        if (duplicatedConsumableConfigurations.isNotEmpty()) {
            val msg = duplicatedConsumableConfigurations.joinToString(separator = "\n") { configs ->
                val list = configs.joinToString { it.name }
                " * $list"
            }
            fail("Following configurations have the same attributes:\n$msg")
        }
    }
}