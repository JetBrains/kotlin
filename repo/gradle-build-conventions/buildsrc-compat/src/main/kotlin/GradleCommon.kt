/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.attributes.plugin.GradlePluginApiVersion
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin
import org.gradle.plugin.devel.tasks.ValidatePlugins
import org.jetbrains.dokka.DokkaVersion
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.GradleExternalDocumentationLinkBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleJavaTargetExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes
import java.net.URL

/**
 * Gradle's plugins common variants.
 *
 * [minimalSupportedGradleVersion] - minimal Gradle version that is supported in this variant
 * [gradleApiVersion] - Gradle API dependency version. Usually should be the same as [minimalSupportedGradleVersion].
 */
enum class GradlePluginVariant(
    val sourceSetName: String,
    val minimalSupportedGradleVersion: String,
    val gradleApiVersion: String,
    val gradleApiJavadocUrl: String
) {
    GRADLE_MIN("main", "6.8.3", "6.9", "https://docs.gradle.org/6.9.3/javadoc/"),
    GRADLE_70("gradle70", "7.0", "7.0", "https://docs.gradle.org/7.0.2/javadoc/"),
    GRADLE_71("gradle71", "7.1", "7.1", "https://docs.gradle.org/7.1.1/javadoc/"),
    GRADLE_74("gradle74", "7.4", "7.4", "https://docs.gradle.org/7.4.2/javadoc/"),
    GRADLE_75("gradle75", "7.5", "7.5", "https://docs.gradle.org/7.5.1/javadoc/"),
    GRADLE_76("gradle76", "7.6", "7.6", "https://docs.gradle.org/7.6.1/javadoc/"),
    GRADLE_80("gradle80", "8.0", "8.0", "https://docs.gradle.org/8.0.2/javadoc/"),
    GRADLE_81("gradle81", "8.1", "8.1", "https://docs.gradle.org/8.1.1/javadoc/"),
}

val commonSourceSetName = "common"

/**
 * Configures common pom configuration parameters
 */
fun Project.configureCommonPublicationSettingsForGradle(
    signingRequired: Boolean,
    sbom: Boolean = true,
) {
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications
                .withType<MavenPublication>()
                .configureEach {
                    configureKotlinPomAttributes(project)
                    if (sbom && project.name !in internalPlugins) {
                        if (name == "pluginMaven") {
                            val sbomTask = configureSbom(target = "PluginMaven")
                            artifact("$buildDir/spdx/PluginMaven/PluginMaven.spdx.json") {
                                extension = "spdx.json"
                                builtBy(sbomTask)
                            }
                        } else if (name == "Main") {
                            val sbomTask = configureSbom()
                            artifact("$buildDir/spdx/MainPublication/MainPublication.spdx.json") {
                                extension = "spdx.json"
                                builtBy(sbomTask)
                            }
                        }
                    }
                }
        }
    }
    configureDefaultPublishing(signingRequired)
}

/**
 * These dependencies will be provided by Gradle, and we should prevent version conflict
 */
fun Configuration.excludeGradleCommonDependencies() {
    dependencies
        .withType<ModuleDependency>()
        .configureEach {
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-script-runtime")
        }
}

/**
 * Exclude Gradle runtime from given SourceSet configurations.
 */
fun Project.excludeGradleCommonDependencies(sourceSet: SourceSet) {
    configurations[sourceSet.implementationConfigurationName].excludeGradleCommonDependencies()
    configurations[sourceSet.apiConfigurationName].excludeGradleCommonDependencies()
    configurations[sourceSet.runtimeOnlyConfigurationName].excludeGradleCommonDependencies()
}

private val internalPlugins = setOf(
    "android-test-fixes",
    "gradle-warnings-detector",
    "kotlin-compiler-args-properties",
)

private val testPlugins = internalPlugins + setOf(
    "kotlin-gradle-plugin-api",
    "kotlin-gradle-plugin",
)

/**
 * Common sources for all variants.
 * Should contain classes that are independent of Gradle API version or using minimal supported Gradle api.
 */
fun Project.createGradleCommonSourceSet(): SourceSet {
    val commonSourceSet = sourceSets.create(commonSourceSetName) {
        excludeGradleCommonDependencies(this)

        // Adding Gradle API to separate configuration, so version will not leak into variants
        val commonGradleApiConfiguration = configurations.create("commonGradleApiCompileOnly") {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        configurations[compileClasspathConfigurationName].extendsFrom(commonGradleApiConfiguration)

        dependencies {
            compileOnlyConfigurationName(kotlinStdlib())
            "commonGradleApiCompileOnly"("dev.gradleplugins:gradle-api:8.2")
            if (this@createGradleCommonSourceSet.name !in testPlugins) {
                compileOnlyConfigurationName(project(":kotlin-gradle-plugin-api")) {
                    capabilities {
                        requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-api-common")
                    }
                }
            }
        }
    }

    plugins.withType<JavaLibraryPlugin>().configureEach {
        this@createGradleCommonSourceSet.extensions.configure<JavaPluginExtension> {
            registerFeature(commonSourceSet.name) {
                usingSourceSet(commonSourceSet)
                disablePublication()
            }
        }
    }

    // Common outputs will also produce '${project.name}.kotlin_module' file, so we need to avoid
    // files clash
    tasks.named<KotlinCompile>("compile${commonSourceSet.name.replaceFirstChar { it.uppercase() }}Kotlin") {
        @Suppress("DEPRECATION")
        kotlinOptions {
            moduleName = "${this@createGradleCommonSourceSet.name}_${commonSourceSet.name}"
        }
    }

    registerValidatePluginTasks(commonSourceSet)

    return commonSourceSet
}

/**
 * Fixes wired SourceSet does not expose compiled common classes and common resources as secondary variant
 * which is used in the Kotlin Project compilation.
 */
private fun Project.fixWiredSourceSetSecondaryVariants(
    wireSourceSet: SourceSet,
    commonSourceSet: SourceSet,
) {
    configurations
        .matching {
            it.name == wireSourceSet.apiElementsConfigurationName ||
                    it.name == wireSourceSet.runtimeElementsConfigurationName
        }
        .configureEach {
            outgoing {
                variants.maybeCreate("classes").apply {
                    attributes {
                        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
                    }
                    (commonSourceSet.output.classesDirs.files + wireSourceSet.output.classesDirs.files)
                        .toSet()
                        .forEach {
                            if (!artifacts.files.contains(it)) {
                                artifact(it) {
                                    type = ArtifactTypeDefinition.JVM_CLASS_DIRECTORY
                                }
                            }
                        }
                }
            }
        }

    configurations
        .matching { it.name == wireSourceSet.runtimeElementsConfigurationName }
        .configureEach {
            outgoing {
                val resourcesDirectories = listOfNotNull(
                    commonSourceSet.output.resourcesDir,
                    wireSourceSet.output.resourcesDir
                )

                if (resourcesDirectories.isNotEmpty()) {
                    variants.maybeCreate("resources").apply {
                        attributes {
                            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.RESOURCES))
                        }
                        resourcesDirectories.forEach {
                            if (!artifacts.files.contains(it)) {
                                artifact(it) {
                                    type = ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY
                                }
                            }
                        }
                    }
                }
            }
        }
}

/**
 * Make [wireSourceSet] to extend [commonSourceSet].
 */
fun Project.wireGradleVariantToCommonGradleVariant(
    wireSourceSet: SourceSet,
    commonSourceSet: SourceSet,
) {
    wireSourceSet.compileClasspath += commonSourceSet.output
    wireSourceSet.runtimeClasspath += commonSourceSet.output

    // Allowing to use 'internal' classes/methods from common source code
    (extensions.getByName("kotlin") as KotlinSingleJavaTargetExtension).target.compilations.run {
        getByName(wireSourceSet.name).associateWith(getByName(commonSourceSet.name))
    }

    configurations[wireSourceSet.apiConfigurationName].extendsFrom(
        configurations[commonSourceSet.apiConfigurationName]
    )
    configurations[wireSourceSet.implementationConfigurationName].extendsFrom(
        configurations[commonSourceSet.implementationConfigurationName]
    )
    configurations[wireSourceSet.runtimeOnlyConfigurationName].extendsFrom(
        configurations[commonSourceSet.runtimeOnlyConfigurationName]
    )
    configurations[wireSourceSet.compileOnlyConfigurationName].extendsFrom(
        configurations[commonSourceSet.compileOnlyConfigurationName]
    )

    fixWiredSourceSetSecondaryVariants(wireSourceSet, commonSourceSet)

    tasks.withType<Jar>().configureEach {
        if (name == wireSourceSet.jarTaskName) {
            from(wireSourceSet.output, commonSourceSet.output)
            setupPublicJar(archiveBaseName.get())
            addEmbeddedRuntime()
            addEmbeddedRuntime(wireSourceSet.embeddedConfigurationName)
        } else if (name == wireSourceSet.sourcesJarTaskName) {
            from(wireSourceSet.allSource, commonSourceSet.allSource)
        }
    }
}

private const val FIXED_CONFIGURATION_SUFFIX = "WithFixedAttribute"

/**
 * 'main' sources are used for minimal supported Gradle versions (6.7) up to Gradle 7.0.
 */
fun Project.reconfigureMainSourcesSetForGradlePlugin(
    commonSourceSet: SourceSet,
) {
    sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME) {
        plugins.withType<JavaGradlePluginPlugin>().configureEach {
            // Removing Gradle api default dependency added by 'java-gradle-plugin'
            configurations[apiConfigurationName].dependencies.remove(dependencies.gradleApi())
        }

        dependencies {
            "compileOnly"(kotlinStdlib())
            // Decoupling gradle-api artifact from current project Gradle version. Later would be useful for
            // gradle plugin variants
            "compileOnly"("dev.gradleplugins:gradle-api:${GradlePluginVariant.GRADLE_MIN.gradleApiVersion}")
            if (this@reconfigureMainSourcesSetForGradlePlugin.name !in testPlugins) {
                "api"(project(":kotlin-gradle-plugin-api"))
            }
        }

        excludeGradleCommonDependencies(this)
        wireGradleVariantToCommonGradleVariant(this, commonSourceSet)

        // https://youtrack.jetbrains.com/issue/KT-51913
        // Remove workaround after bootstrap update
        if (configurations["default"].attributes.contains(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE)) {
            configurations["default"].attributes.attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment::class, "no-op")
            )
        }

        plugins.withType<JavaLibraryPlugin>().configureEach {
            this@reconfigureMainSourcesSetForGradlePlugin
                .extensions
                .configure<JavaPluginExtension> {
                    withSourcesJar()
                    if (kotlinBuildProperties.publishGradlePluginsJavadoc) {
                        withJavadocJar()
                    }
                }

            configurations.create(sourceSets.getByName("main").embeddedConfigurationName) {
                isCanBeConsumed = false
                isCanBeResolved = true
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                }
            }
        }

        // Workaround for https://youtrack.jetbrains.com/issue/KT-52987
        val javaComponent = project.components["java"] as AdhocComponentWithVariants
        listOf(
            runtimeElementsConfigurationName,
            apiElementsConfigurationName
        )
            .map { configurations[it] }
            .forEach { originalConfiguration ->
                configurations.create("${originalConfiguration.name}$FIXED_CONFIGURATION_SUFFIX") {
                    isCanBeResolved = originalConfiguration.isCanBeResolved
                    isCanBeConsumed = originalConfiguration.isCanBeConsumed
                    isVisible = originalConfiguration.isVisible
                    setExtendsFrom(originalConfiguration.extendsFrom)

                    artifacts {
                        originalConfiguration.artifacts.forEach {
                            add(name, it)
                        }
                    }

                    // Removing 'org.jetbrains.kotlin.platform.type' attribute
                    // as it brings issues with Gradle variant resolve on Gradle 7.6+ versions
                    attributes {
                        originalConfiguration.attributes.keySet()
                            .filter { it.name != KotlinPlatformType.attribute.name }
                            .forEach { originalAttribute ->
                                @Suppress("UNCHECKED_CAST")
                                attribute(
                                    originalAttribute as Attribute<Any>,
                                    originalConfiguration.attributes.getAttribute(originalAttribute)!!
                                )
                            }

                        plugins.withType<JavaPlugin> {
                            tasks.named<JavaCompile>(compileJavaTaskName).get().apply {
                                attribute(
                                    TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
                                    when (targetCompatibility) {
                                        "1.8" -> 8
                                        else -> targetCompatibility.toInt()
                                    }
                                )
                            }
                        }
                    }

                    val expectedAttributes = setOf(
                        Category.CATEGORY_ATTRIBUTE,
                        Bundling.BUNDLING_ATTRIBUTE,
                        Usage.USAGE_ATTRIBUTE,
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                        TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE
                    )
                    if (attributes.keySet() != expectedAttributes) {
                        error(
                            "Wrong set of attributes:\n" +
                                    "  Expected: ${expectedAttributes.joinToString(", ")}\n" +
                                    "  Actual: ${attributes.keySet().joinToString(", ") { "${it.name}=${attributes.getAttribute(it)}" }}"
                        )
                    }

                    javaComponent.addVariantsFromConfiguration(this) {
                        mapToMavenScope(
                            when (originalConfiguration.name) {
                                runtimeElementsConfigurationName -> "runtime"
                                apiElementsConfigurationName -> "compile"
                                else -> error("Unsupported configuration name")
                            }
                        )
                    }

                    // Make original configuration unpublishable and not visible
                    originalConfiguration.isCanBeConsumed = false
                    originalConfiguration.isVisible = false
                    javaComponent.withVariantsFromConfiguration(originalConfiguration) {
                        skip()
                    }
                }
            }
    }

    // Fix common sources visibility for tests
    sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME) {
        compileClasspath += commonSourceSet.output
        runtimeClasspath += commonSourceSet.output
    }

    // Allowing to use 'internal' classes/methods from common source code
    (extensions.getByName("kotlin") as KotlinSingleJavaTargetExtension).target.compilations.run {
        getByName(SourceSet.TEST_SOURCE_SET_NAME).associateWith(getByName(commonSourceSet.name))
    }
}

/**
 * Adding plugin variants: https://docs.gradle.org/current/userguide/implementing_gradle_plugins.html#plugin-with-variants
 */
fun Project.createGradlePluginVariant(
    variant: GradlePluginVariant,
    commonSourceSet: SourceSet,
    isGradlePlugin: Boolean = true,
): SourceSet {
    val variantSourceSet = sourceSets.create(variant.sourceSetName) {
        excludeGradleCommonDependencies(this)
        wireGradleVariantToCommonGradleVariant(this, commonSourceSet)
    }

    plugins.withType<JavaLibraryPlugin>().configureEach {
        extensions.configure<JavaPluginExtension> {
            registerFeature(variantSourceSet.name) {
                usingSourceSet(variantSourceSet)
                if (isGradlePlugin) {
                    capability(project.group.toString(), project.name, project.version.toString())
                }

                if (kotlinBuildProperties.publishGradlePluginsJavadoc) {
                    withJavadocJar()
                }
                withSourcesJar()
            }

            configurations.named(variantSourceSet.apiElementsConfigurationName, commonVariantAttributes())
            configurations.named(variantSourceSet.runtimeElementsConfigurationName, commonVariantAttributes())

            configurations.create(variantSourceSet.embeddedConfigurationName) {
                isCanBeConsumed = false
                isCanBeResolved = true
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                }
            }
        }

        tasks.named<Jar>(variantSourceSet.sourcesJarTaskName) {
            addEmbeddedSources()
            addEmbeddedSources(variantSourceSet.embeddedConfigurationName)
        }
    }

    plugins.withId("java-gradle-plugin") {
        tasks.named<Copy>(variantSourceSet.processResourcesTaskName) {
            val copyPluginDescriptors = rootSpec.addChild()
            copyPluginDescriptors.into("META-INF/gradle-plugins")
            copyPluginDescriptors.from(tasks.named("pluginDescriptors"))
        }
    }

    configurations.configureEach {
        if (this@configureEach.name.startsWith(variantSourceSet.name)) {
            attributes {
                attribute(
                    GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                    objects.named(variant.minimalSupportedGradleVersion)
                )
            }
        }
    }

    // KT-52138: Make module name the same for all variants, so KSP could access internal methods/properties
    tasks.named<KotlinCompile>("compile${variantSourceSet.name.replaceFirstChar { it.uppercase() }}Kotlin") {
        @Suppress("DEPRECATION")
        kotlinOptions {
            moduleName = this@createGradlePluginVariant.name
        }
    }

    dependencies {
        variantSourceSet.compileOnlyConfigurationName(kotlinStdlib())
        variantSourceSet.compileOnlyConfigurationName("dev.gradleplugins:gradle-api:${variant.gradleApiVersion}")
        if (this@createGradlePluginVariant.name !in testPlugins) {
            variantSourceSet.apiConfigurationName(project(":kotlin-gradle-plugin-api")) {
                capabilities {
                    requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-api-${variant.sourceSetName}")
                }
            }
        }
    }

    registerValidatePluginTasks(variantSourceSet)

    return variantSourceSet
}

/**
 * All additional configuration attributes in plugin variant should be the same as in the 'main' variant.
 * Otherwise, Gradle <7.0 will fail to select plugin variant.
 */
private fun Project.commonVariantAttributes(): Action<Configuration> = Action<Configuration> {
    attributes {
        attribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            objects.named(TargetJvmEnvironment.STANDARD_JVM)
        )
    }
}

fun Project.configureKotlinCompileTasksGradleCompatibility() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            // check https://docs.gradle.org/current/userguide/compatibility.html#kotlin for Kotlin-Gradle versions matrix
            @Suppress("DEPRECATION") // we can't use language version greater than 1.5 as minimal supported Gradle embeds Kotlin 1.4
            languageVersion.set(KotlinVersion.KOTLIN_1_5)
            @Suppress("DEPRECATION") // we can't use api version greater than 1.4 as minimal supported Gradle version uses kotlin-stdlib 1.4
            apiVersion.set(KotlinVersion.KOTLIN_1_4)
            freeCompilerArgs.addAll(
                listOf(
                    "-Xskip-prerelease-check",
                    "-Xsuppress-version-warnings",
                    // We have to override the default value for `-Xsam-conversions` to `class`
                    // otherwise the compiler would compile lambdas using invokedynamic,
                    // such lambdas are not serializable so are not compatible with Gradle configuration cache.
                    // It doesn't lead to a significant difference in binaries sizes, and previously (before LV 1.5) the `class` value was set by default.
                    "-Xsam-conversions=class",
                )
            )
        }
    }
}

// Will allow combining outputs of multiple SourceSets
fun Project.publishShadowedJar(
    sourceSet: SourceSet,
    commonSourceSet: SourceSet,
) {
    val jarTask = tasks.named<Jar>(sourceSet.jarTaskName)

    val shadowJarTask = embeddableCompilerDummyForDependenciesRewriting(
        taskName = "$EMBEDDABLE_COMPILER_TASK_NAME${sourceSet.jarTaskName.replaceFirstChar { it.uppercase() }}"
    ) {
        setupPublicJar(
            jarTask.flatMap { it.archiveBaseName },
            jarTask.flatMap { it.archiveClassifier }
        )
        addEmbeddedRuntime()
        addEmbeddedRuntime(sourceSet.embeddedConfigurationName)
        from(sourceSet.output)
        from(commonSourceSet.output)

        // When Gradle traverses the inputs, reject the shaded compiler JAR,
        // which leads to the content of that JAR being excluded as well:
        exclude {
            // Docstring says `file` never returns null, but it does
            @Suppress("UNNECESSARY_SAFE_CALL", "SAFE_CALL_WILL_CHANGE_NULLABILITY")
            it.file?.name?.startsWith("kotlin-compiler-embeddable") ?: false
        }
    }

    // Removing artifact produced by Jar task
    if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) {
        configurations["${sourceSet.runtimeElementsConfigurationName}$FIXED_CONFIGURATION_SUFFIX"]
            .artifacts.removeAll { true }
        configurations["${sourceSet.apiElementsConfigurationName}$FIXED_CONFIGURATION_SUFFIX"]
            .artifacts.removeAll { true }
    } else {
        configurations[sourceSet.runtimeElementsConfigurationName]
            .artifacts.removeAll { true }
        configurations[sourceSet.apiElementsConfigurationName]
            .artifacts.removeAll { true }
    }

    // Adding instead artifact from shadow jar task
    configurations {
        artifacts {
            if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) {
                add("${sourceSet.runtimeElementsConfigurationName}$FIXED_CONFIGURATION_SUFFIX", shadowJarTask)
                add("${sourceSet.apiElementsConfigurationName}$FIXED_CONFIGURATION_SUFFIX", shadowJarTask)
            } else {
                add(sourceSet.apiElementsConfigurationName, shadowJarTask)
                add(sourceSet.runtimeElementsConfigurationName, shadowJarTask)
            }
        }
    }
}

fun Project.addBomCheckTask() {
    val checkBomTask = tasks.register("checkGradlePluginsBom") {
        group = "Validation"
        description = "Check if project is added into Kotlin Gradle Plugins bom"

        val bomBuildFile = project(":kotlin-gradle-plugins-bom").projectDir.resolve("build.gradle.kts")
        val exceptions = listOf(
            project(":gradle:android-test-fixes").path,
            project(":gradle:gradle-warnings-detector").path,
            project(":gradle:kotlin-compiler-args-properties").path,
            project(":kotlin-gradle-build-metrics").path,
            project(":kotlin-gradle-statistics").path,
        )
        val projectPath = this@addBomCheckTask.path

        doLast {
            if (projectPath in exceptions) return@doLast

            val constraintsLines = bomBuildFile.readText()
                .substringAfter("constraints {")
                .substringBefore("}")
                .split("\n")
                .map { it.trim() }

            val isContainingThisProject = constraintsLines.contains(
                "api(project(\"$projectPath\"))"
            )

            if (!isContainingThisProject) {
                throw GradleException(":kotlin-gradle-plugins-bom does not contain $projectPath project constraint!")
            }
        }
    }

    tasks.named("check") {
        dependsOn(checkBomTask)
    }
}

fun Project.configureDokkaPublication(
    shouldLinkGradleApi: Boolean = false,
    configurePublishingToKotlinlang: Boolean = false,
) {

    val dokkaVersioningPluginVersion = "1.8.10"

    dependencies {
        implicitDependencies("org.jetbrains.dokka:javadoc-plugin:${DokkaVersion.version}")
        implicitDependencies("org.jetbrains.dokka:versioning-plugin:$dokkaVersioningPluginVersion")
    }

    if (!kotlinBuildProperties.publishGradlePluginsJavadoc) return

    plugins.apply("org.jetbrains.dokka")
    plugins.withId("org.jetbrains.dokka") {
        val commonSourceSet = sourceSets.getByName(commonSourceSetName)

        GradlePluginVariant.values().forEach { pluginVariant ->
            val variantSourceSet = sourceSets.getByName(pluginVariant.sourceSetName)
            val dokkaTaskName = "dokka${variantSourceSet.javadocTaskName.replaceFirstChar { it.uppercase() }}"

            val dokkaTask = if (tasks.names.contains(dokkaTaskName)) {
                tasks.named<DokkaTask>(dokkaTaskName)
            } else {
                tasks.register<DokkaTask>(dokkaTaskName)
            }
            dokkaTask.configure {
                description = "Generates documentation in 'javadoc' format for '${variantSourceSet.javadocTaskName}' variant"

                plugins.dependencies.add(
                    project.dependencies.create("org.jetbrains.dokka:javadoc-plugin:${DokkaVersion.version}")
                )

                dokkaSourceSets {
                    named(commonSourceSet.name) {
                        suppress.set(false)
                        jdkVersion.set(8)
                    }

                    named(variantSourceSet.name) {
                        dependsOn(commonSourceSet)
                        suppress.set(false)
                        jdkVersion.set(8)

                        if (shouldLinkGradleApi) {
                            externalDocumentationLink {
                                url.set(URL(pluginVariant.gradleApiJavadocUrl))

                                addWorkaroundForElementList(pluginVariant)
                            }
                        }
                    }
                }
            }

            tasks.named<Jar>(variantSourceSet.javadocJarTaskName) {
                from(dokkaTask.flatMap { it.outputDirectory })
            }
        }

        if (configurePublishingToKotlinlang) {
            val latestVariant = GradlePluginVariant.values().last()
            val olderVersionsDir = layout.buildDirectory.dir("dokka/kotlinlangDocumentationOld")

            project.dependencies {
                // Version is required due to https://github.com/Kotlin/dokka/issues/2812
                "dokkaHtmlPlugin"("org.jetbrains.dokka:versioning-plugin:$dokkaVersioningPluginVersion")
            }

            tasks.register<DokkaTask>("dokkaKotlinlangDocumentation") {
                description = "Generates documentation reference for Kotlinlang"

                dokkaSourceSets {
                    pluginsMapConfiguration.put(
                        "org.jetbrains.dokka.base.DokkaBase",
                        "{ \"templatesDir\": \"${layout.projectDirectory.dir("dokka-template")}\" }"
                    )
                    pluginsMapConfiguration.put(
                        "org.jetbrains.dokka.versioning.VersioningPlugin",
                        olderVersionsDir.map { olderVersionsDir ->
                            "{ \"version\":\"$version\", \"olderVersionsDir\":\"${olderVersionsDir.asFile}\" }"
                        }
                    )

                    named(commonSourceSet.name) {
                        suppress.set(false)
                        jdkVersion.set(8)
                    }

                    named(latestVariant.sourceSetName) {
                        dependsOn(commonSourceSet)
                        suppress.set(false)
                        jdkVersion.set(8)

                        if (shouldLinkGradleApi) {
                            externalDocumentationLink {
                                url.set(URL(latestVariant.gradleApiJavadocUrl))

                                addWorkaroundForElementList(latestVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Workaround for https://github.com/Kotlin/dokka/issues/2097
// Gradle 7.6 javadoc does not have published 'package-list' file
private fun GradleExternalDocumentationLinkBuilder.addWorkaroundForElementList(pluginVariant: GradlePluginVariant) {
    if (pluginVariant == GradlePluginVariant.GRADLE_76 ||
        pluginVariant == GradlePluginVariant.GRADLE_80 ||
        pluginVariant == GradlePluginVariant.GRADLE_81
    ) {
        packageListUrl.set(URL("${pluginVariant.gradleApiJavadocUrl}element-list"))
    }
}

private val SourceSet.embeddedConfigurationName get() = "${name}Embedded"

// We want to still validate Gradle types without applying `java-gradle-plugin`
// Following configuration is a copy of configuration for the task done by the `java-gradle-plugin`
fun Project.registerValidatePluginTasks(
    sourceSet: SourceSet
): TaskProvider<ValidatePlugins> {
    val validatePluginsTask = tasks.register<ValidatePlugins>("validatePlugins${sourceSet.name.capitalize()}") {
        group = "Plugin development" // PLUGIN_DEVELOPMENT_GROUP
        // VALIDATE_PLUGIN_TASK_DESCRIPTION
        description = "Validates the plugin by checking parameter annotations on task and artifact transform types etc."

        enableStricterValidation.set(true)
        failOnWarning.set(true)
        outputFile.set(project.layout.buildDirectory.file("reports/plugin-development/validation-report-${sourceSet.name}.txt"))
        classes.from({ sourceSet.output.classesDirs })
        classpath.from({ sourceSet.compileClasspath })

        val javaPluginExtension = project.extensions.getByType<JavaPluginExtension>()
        val toolchainService = project.extensions.getByType<JavaToolchainService>()
        launcher.convention(toolchainService.launcherFor(javaPluginExtension.toolchain))
    }

    tasks.named(JavaBasePlugin.CHECK_TASK_NAME) {
        dependsOn(validatePluginsTask)
    }

    tasks.named("test") {
        dependsOn(validatePluginsTask)
    }

    return validatePluginsTask
}
