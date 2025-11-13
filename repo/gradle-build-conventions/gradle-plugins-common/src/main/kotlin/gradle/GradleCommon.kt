/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import gradle.commonSourceSetName
import gradle.GradlePluginVariant
import gradle.publishGradlePluginsJavadoc
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.attributes.plugin.GradlePluginApiVersion
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugin.devel.PluginDeclaration
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin
import org.gradle.plugin.devel.tasks.ValidatePlugins
import org.gradle.plugin.use.resolve.internal.ArtifactRepositoriesPluginResolver.PLUGIN_MARKER_SUFFIX
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleJavaTargetExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes
import java.io.File

/**
 * We have to handle the returned provider lazily, because the publication's artifactId
 * and the plugin declaration may be not yet configured,
 * so we may be not able to find the related plugin declaration eagerly.
 */
private fun Project.getPluginDeclarationFor(mavenPublication: MavenPublication): Provider<PluginDeclaration> {
    return project.provider {
        val pluginDevelopment = extensions.findByType(GradlePluginDevelopmentExtension::class)
            ?: error("Plugin marker publication $name detected without the `java-gradle-plugin` plugin")
        pluginDevelopment.plugins
            .find { pluginDeclaration -> "${pluginDeclaration.id}${PLUGIN_MARKER_SUFFIX}" == mavenPublication.artifactId }
            ?: error("Cannot find plugin declaration for publication ${this.name} (${mavenPublication.groupId}:${mavenPublication.artifactId})")
    }
}

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
                    val pluginDeclaration: Provider<PluginDeclaration>? =
                        if (name.endsWith("PluginMarkerMaven")) {
                            getPluginDeclarationFor(this)
                        } else {
                            null
                        }
                    configureKotlinPomAttributes(
                        project,
                        explicitName = pluginDeclaration?.map { it.displayName } ?: provider { null },
                        explicitDescription = pluginDeclaration?.map { it.description } ?: provider { null },
                    )
                    if (sbom && project.name !in internalPlugins) {
                        if (name == "pluginMaven") {
                            val sbomTask = configureSbom(target = "PluginMaven")
                            artifact(sbomTask) {
                                extension = "spdx.json"
                                builtBy(sbomTask)
                            }
                        } else if (name == "Main") {
                            val sbomTask = configureSbom()
                            artifact(sbomTask) {
                                extension = "spdx.json"
                                builtBy(sbomTask)
                            }
                        }
                    }
                }
        }
        configureDefaultPublishing(signingRequired)
    }
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
 * Should contain classes that are independent of the Gradle API version or using the maximum supported Gradle API.
 */
fun Project.createGradleCommonSourceSet(): SourceSet {
    val commonSourceSet = sourceSets.create(commonSourceSetName) {
        excludeGradleCommonDependencies(this)

        repositories {
            exclusiveContent {
                forRepository {
                    maven(url = "https://repo.gradle.org/gradle/libs-releases")
                }
                filter {
                    includeGroup("org.gradle.experimental")
                }
            }
        }

        // Adding Gradle API to separate configuration, so version will not leak into variants
        val commonGradleApiConfiguration = configurations.dependencyScope("commonGradleApiCompileOnly")

        configurations.named(compileClasspathConfigurationName) {
            extendsFrom(commonGradleApiConfiguration.get())
            // Overriding current project Gradle version to the version common sources compiled against
            attributes {
                attribute(
                    GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                    objects.named(GradlePluginVariant.GRADLE_COMMON_COMPILE_API_VERSION),
                )
            }
        }

        dependencies {
            compileOnlyConfigurationName("org.jetbrains.kotlin:kotlin-stdlib:${GradlePluginVariant.GRADLE_MIN.bundledKotlinVersion}.0")
            "commonGradleApiCompileOnly"("org.gradle.experimental:gradle-public-api:${GradlePluginVariant.GRADLE_COMMON_COMPILE_API_VERSION}") {
                capabilities {
                    requireCapability("org.gradle.experimental:gradle-public-api-internal")
                }
            }
            if (this@createGradleCommonSourceSet.name !in testPlugins) {
                compileOnlyConfigurationName(project(":kotlin-gradle-plugin-api"))
            }
        }
    }

    afterEvaluate {
        // The common source set compilation artifacts are never intended for runtime consumption
        configurations.getByName(commonSourceSet.runtimeElementsConfigurationName).isCanBeConsumed = false

        listOf(
            /**
             * Common source set outgoing variants should be a superset of attributes relative to [FIXED_CONFIGURATION_SUFFIX]. These
             * variants are not published, but they are used to resolve interproject dependencies
             */
            commonSourceSet.apiElementsConfigurationName,
        ).forEach {
            val outgoingVariant = configurations.getByName(it)
            commonVariantAttributes().execute(outgoingVariant)
            outgoingVariant.attributes {
                attribute(
                    GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                    objects.named(GradlePluginVariant.GRADLE_COMMON_COMPILE_API_VERSION),
                )
            }
        }
    }

    plugins.withType<JavaLibraryPlugin>().configureEach {
        this@createGradleCommonSourceSet.extensions.configure<JavaPluginExtension> {
            registerFeature(commonSourceSet.name) {
                usingSourceSet(commonSourceSet)
                capability(project.group.toString(), project.name, project.version.toString())
                disablePublication()
            }
        }
    }
    val kotlinJvmTarget = (extensions.getByName("kotlin") as KotlinSingleJavaTargetExtension).target
    val compilation = kotlinJvmTarget.compilations.getByName(commonSourceSet.name)
    tasks.named<KotlinCompile>(compilation.compileKotlinTaskName) {
        // Common outputs will also produce '${project.name}.kotlin_module' file, so we need to avoid
        // files clash
        compilerOptions.moduleName.set("${this@createGradleCommonSourceSet.name}_${commonSourceSet.name}")
        // Workaround for https://youtrack.jetbrains.com/issue/KT-80750
        compilerOptions.freeCompilerArgs.add("-Xjspecify-annotations=ignore")
        configureGradleCompatibility()
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
            "compileOnly"("org.jetbrains.kotlin:kotlin-stdlib:${GradlePluginVariant.GRADLE_MIN}.0")
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

        listOf(
            compileClasspathConfigurationName,
            runtimeClasspathConfigurationName,
        ).forEach {
            configurations.getByName(it).useDependenciesCompiledForGradle(
                GradlePluginVariant.GRADLE_MIN,
                objects,
            )
        }
    }

    val kotlinJvmTarget = (extensions.getByName("kotlin") as KotlinSingleJavaTargetExtension).target
    val mainCompilation = kotlinJvmTarget.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
    tasks.named<KotlinCompile>(mainCompilation.compileKotlinTaskName) {
        configureGradleCompatibility()
    }

    // Fix common sources visibility for tests
    sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME) {
        compileClasspath += commonSourceSet.output
        runtimeClasspath += commonSourceSet.output
    }

    // Allowing to use 'internal' classes/methods from common source code
    kotlinJvmTarget.compilations.run {
        getByName(SourceSet.TEST_SOURCE_SET_NAME).associateWith(getByName(commonSourceSet.name))
    }
}

/**
 * Adding plugin variants: https://docs.gradle.org/current/userguide/implementing_gradle_plugins.html#plugin-with-variants
 */
private fun Project.createGradlePluginVariant(
    variant: GradlePluginVariant,
    commonSourceSet: SourceSet,
    publishShadowedJar: Boolean,
): SourceSet {
    val variantSourceSet = sourceSets.create(variant.sourceSetName) {
        excludeGradleCommonDependencies(this)
        wireGradleVariantToCommonGradleVariant(this, commonSourceSet)
    }

    plugins.withType<JavaLibraryPlugin>().configureEach {
        extensions.configure<JavaPluginExtension> {
            registerFeature(variantSourceSet.name) {
                usingSourceSet(variantSourceSet)
                capability(project.group.toString(), project.name, project.version.toString())

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
        if (this@configureEach.name.startsWith(variantSourceSet.name) && (isCanBeResolved || isCanBeConsumed)) {
            useDependenciesCompiledForGradle(
                variant,
                objects,
            )
        }
    }

    val kotlinJvmTarget = (extensions.getByName("kotlin") as KotlinSingleJavaTargetExtension).target
    val compilation = kotlinJvmTarget.compilations.getByName(variantSourceSet.name)
    tasks.named<KotlinCompile>(compilation.compileKotlinTaskName) {
        // KT-52138: Make module name the same for all variants, so KSP could access internal methods/properties
        compilerOptions.moduleName.set(this@createGradlePluginVariant.name)
        configureGradleCompatibility()
    }

    dependencies {
        variantSourceSet.compileOnlyConfigurationName("org.jetbrains.kotlin:kotlin-stdlib:${GradlePluginVariant.GRADLE_MIN.bundledKotlinVersion}.0")
        if (variant == GradlePluginVariant.GRADLE_813) {
            // Workaround until 'dev.gradleplugins:gradle-api:8.13' will be published
            variantSourceSet.compileOnlyConfigurationName("org.jetbrains.intellij.deps:gradle-api:${variant.gradleApiVersion}")
            variantSourceSet.compileOnlyConfigurationName("javax.inject:javax.inject:1")
        } else {
            variantSourceSet.compileOnlyConfigurationName("dev.gradleplugins:gradle-api:${variant.gradleApiVersion}")
        }
        if (this@createGradlePluginVariant.name !in testPlugins) {
            variantSourceSet.apiConfigurationName(project(":kotlin-gradle-plugin-api"))
        }
    }

    registerValidatePluginTasks(variantSourceSet)

    if (publishShadowedJar) {
        publishShadowedJar(variantSourceSet, commonSourceSet)
    }

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

/**
 * Configures the JVM compile task to produce binaries compatible with [GradlePluginVariant.GRADLE_MIN].
 */
fun KotlinCompile.configureGradleCompatibility() {
    compilerOptions {
        val variant = GradlePluginVariant.GRADLE_MIN
        // we should keep control of the language version for compatibility with bundled Kotlin compiler for Gradle Kotlin scripts.
        languageVersion.set(KotlinVersion.fromVersion(variant.bundledKotlinVersion))
        // we should not use stdlib symbols not available in the bundled Kotlin runtime
        apiVersion.set(KotlinVersion.fromVersion(variant.bundledKotlinVersion))
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

/**
 * Configures the main JVM compile task in the project to use specific setup for compatibility with [GradlePluginVariant.GRADLE_MIN]
 * If you need to configure it for specific tasks, please use [configureGradleCompatibility] and [configureBuildToolsApiVersionForGradleCompatibility].
 */
fun Project.configureKotlinCompileTasksGradleCompatibility() {
    configureBuildToolsApiVersionForGradleCompatibility()
    tasks.named("compileKotlin", KotlinCompile::class.java) {
        configureGradleCompatibility()
    }
}

/**
 * Configures the build tools API version for the project to use Kotlin compiler of the version
 * that can produce binaries of [GradlePluginVariant.bundledKotlinVersion] for [GradlePluginVariant.GRADLE_MIN]
 * Reconfigures API and language versions to the defaults for this compiler version.
 */
@OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
fun Project.configureBuildToolsApiVersionForGradleCompatibility() {
    if (extra.properties["avoidSettingCompilerVersionForBTA"].toString().toBoolean()) return
    val catalogs = extensions.getByType<VersionCatalogsExtension>()
    val libsCatalog = catalogs.named("libs")
    val kgpCompilerVersion = libsCatalog.findVersion("kotlin.for.gradle.plugins.compilation").get().requiredVersion
    (extensions.getByName("kotlin") as KotlinBaseExtension).compilerVersion.set(kgpCompilerVersion)
    tasks.withType<KotlinCompile>().configureEach {
        compilerExecutionStrategy.set(KotlinCompilerExecutionStrategy.IN_PROCESS) // avoid spawning multiple Kotlin daemons for different bootstrap versions
        compilerOptions {
            val kgpCompilerMajorVersion = kgpCompilerVersion.substringBeforeLast(".").let { KotlinVersion.fromVersion(it) }
            languageVersion.set(kgpCompilerMajorVersion)
            apiVersion.set(kgpCompilerMajorVersion)
        }
    }
    project.extra["kotlin.compiler.runViaBuildToolsApi"] = true
}

// Will allow combining outputs of multiple SourceSets
fun Project.publishShadowedJar(
    sourceSet: SourceSet,
    commonSourceSet: SourceSet,
) {
    val jarTask = tasks.named<Jar>(sourceSet.jarTaskName)

    val shadowJarTask = tasks.register<ShadowJar>(
        "$EMBEDDABLE_COMPILER_TASK_NAME${sourceSet.jarTaskName.replaceFirstChar { it.uppercase() }}"
    ) {
        destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
        setupPublicJar(
            jarTask.flatMap { it.archiveBaseName },
            jarTask.flatMap { it.archiveClassifier }
        )
        addEmbeddedRuntime()
        addEmbeddedRuntime(sourceSet.embeddedConfigurationName)
        from(sourceSet.output)
        from(commonSourceSet.output)

        configureEmbeddableCompilerRelocation(withJavaxInject = false)
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

private val SourceSet.embeddedConfigurationName get() = "${name}Embedded"

// We want to still validate Gradle types without applying `java-gradle-plugin`
// Following configuration is a copy of configuration for the task done by the `java-gradle-plugin`
fun Project.registerValidatePluginTasks(
    sourceSet: SourceSet,
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

/**
 * Register a kotlin source for a range of Gradle versions [[from], [to]). The sources will be visible to build scripts [from]
 * [GradlePluginVariant.minimalSupportedGradleVersion] up to but not including [to].
 *
 * Use this utility to overwrite or expose some API to build scripts that execute with a range of Gradle versions
 */
fun Project.registerKotlinSourceForVersionRange(
    from: GradlePluginVariant,
    to: GradlePluginVariant,
) {
    if (GradleVersion.version(from.minimalSupportedGradleVersion) >= GradleVersion.version(to.minimalSupportedGradleVersion)) {
        error("Minimal target version from ${from.minimalSupportedGradleVersion} must be < version to ${to.minimalSupportedGradleVersion}")
    }
    val applicableVariants = GradlePluginVariant.values().sortedBy { GradleVersion.version(it.minimalSupportedGradleVersion) }
        .dropWhile { it != from }
        .takeWhile { it != to }
    if (applicableVariants.isEmpty()) {
        error("Ranged source set [${from}, ${to}) applies to no variants")
    }
    val sourceDirectoryName = "from_${applicableVariants.first().sourceSetName}_through_${applicableVariants.last().sourceSetName}"
    val sourcesDirectory = project.layout.projectDirectory.dir("src/${sourceDirectoryName}")
    applicableVariants
        .forEach {
            val sourceSet = sourceSets.getByName(it.sourceSetName)
            (sourceSet.extensions.getByName("kotlin") as SourceDirectorySet).srcDir(sourcesDirectory)
            val kotlinJvmTarget = (extensions.getByName("kotlin") as KotlinSingleJavaTargetExtension).target
            val compilation = kotlinJvmTarget.compilations.getByName(sourceSet.name)
            compilation.compileJavaTaskProvider.configure {
                doFirst {
                    if (sourcesDirectory.asFileTree.isEmpty) {
                        error("Ranged Gradle version sources directory\n$sourcesDirectory\nis empty. Remove ranged version or add sources to the directory")
                    }
                }
            }
        }
}

// See KT-79528 and https://github.com/gradle/gradle/issues/34453
fun Project.disableCoroutinesStacktraceRecoveryInTestsIfGradleEmbeddedStdlibIsInRuntimeClasspath() {
    tasks.withType<Test>().configureEach {
        val isEmbeddedStdlibInRuntimeClasspath = provider {
            classpath.contains(gradleEmbeddedStdlib())
        }
        jvmArgumentProviders.add {
            if (isEmbeddedStdlibInRuntimeClasspath.get()) {
                listOf("-Dkotlinx.coroutines.stacktrace.recovery=false")
            } else {
                emptyList()

            }
        }
    }
}

fun Project.excludeGradleEmbeddedStdlibFromTestTasksRuntimeClasspath() {
    afterEvaluate {
        tasks.withType<Test>().configureEach {
            val embeddedStdlib = gradleEmbeddedStdlib()
            classpath = classpath.filter {
                it != embeddedStdlib
            }
        }
    }
}

private fun Project.gradleEmbeddedStdlib(): File = (dependencies.gradleApi() as FileCollectionDependency).files.single { it.name.startsWith("kotlin-stdlib") }

fun Project.createGradlePluginVariants(
    commonSourceSet: SourceSet,
    publishShadowedJar: Boolean,
) {
    listOf(
        GradlePluginVariant.GRADLE_80,
        GradlePluginVariant.GRADLE_81,
        GradlePluginVariant.GRADLE_82,
        GradlePluginVariant.GRADLE_85,
        GradlePluginVariant.GRADLE_86,
        GradlePluginVariant.GRADLE_88,
        GradlePluginVariant.GRADLE_811,
        GradlePluginVariant.GRADLE_813,
    ).forEach { variant ->
        createGradlePluginVariant(
            variant = variant,
            commonSourceSet = commonSourceSet,
            publishShadowedJar = publishShadowedJar,
        )
    }
}

fun Configuration.useDependenciesCompiledForGradle(
    variant: GradlePluginVariant,
    objects: ObjectFactory,
) = useDependenciesCompiledForGradle(
    variant.minimalSupportedGradleVersion,
    objects,
)

fun Configuration.useDependenciesCompiledForGradle(
    version: String,
    objects: ObjectFactory,
) = attributes.attribute(
    GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
    objects.named(version),
)

/**
 * Allows resolving dependency targeting [jdkMajorVersion] in the project using an incompatible JDK version.
 */
fun ModuleDependency.overrideTargetJvmVersion(jdkMajorVersion: Int) {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, jdkMajorVersion)
    }
}
