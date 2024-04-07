/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig
import org.gradle.api.Action
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import java.io.File

/**
 * Represents a DSL for managing the dependencies of Kotlin entities that implement a [HasKotlinDependencies] interface.
 */
interface KotlinDependencyHandler : HasProject {

    /**
     * Adds an `api` [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies)
     * to this entity.
     *
     * @see [HasKotlinDependencies.apiConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun api(dependencyNotation: Any): Dependency?

    /**
     * Adds an `api` [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies)
     * to this entity.
     *
     * @see [HasKotlinDependencies.apiConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @param configure Additional configuration for the created module dependency.
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun api(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency

    /**
     * Adds an `api` [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies)
     * to this entity.
     *
     * @see [HasKotlinDependencies.apiConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @param configure Additional configuration for the created module dependency.
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun api(dependencyNotation: String, configure: Action<ExternalModuleDependency>): ExternalModuleDependency = api(dependencyNotation) {
        configure.execute(this)
    }

    /**
     * Adds an `api` dependency to this entity.
     *
     * @see [HasKotlinDependencies.apiConfigurationName]
     *
     * @param dependency The dependency to add.
     * @param configure Additional configuration for the [dependency].
     * @return The added [dependency].
     */
    fun <T : Dependency> api(dependency: T, configure: T.() -> Unit): T

    /**
     * Adds an `api` dependency to this entity.
     *
     * @see [HasKotlinDependencies.apiConfigurationName]
     *
     * @param dependency The dependency to add.
     * @param configure Additional configuration for the [dependency].
     * @return The added [dependency].
     */
    fun <T : Dependency> api(dependency: T, configure: Action<T>) = api(dependency) { configure.execute(this) }

    /**
     * Adds an `implementation`
     * [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies) to this entity.
     *
     * @see [HasKotlinDependencies.implementationConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun implementation(dependencyNotation: Any): Dependency?

    /**
     * Adds an `implementation`
     * [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies) to this entity.
     *
     * @see [HasKotlinDependencies.implementationConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @param configure Additional configuration for the created module dependency.
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun implementation(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency

    /**
     * Adds an `implementation`
     * [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies) to this entity.
     *
     * @see [HasKotlinDependencies.implementationConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @param configure additional configuration for the created module dependency.
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun implementation(dependencyNotation: String, configure: Action<ExternalModuleDependency>) =
        implementation(dependencyNotation) { configure.execute(this) }

    /**
     * Adds an `implementation` dependency to this entity.
     *
     * @see [HasKotlinDependencies.implementationConfigurationName]
     *
     * @param dependency The dependency to add.
     * @param configure Additional configuration for the [dependency].
     * @return The added [dependency].
     */
    fun <T : Dependency> implementation(dependency: T, configure: T.() -> Unit): T

    /**
     * Adds an `implementation` dependency to this entity.
     *
     * @see [HasKotlinDependencies.implementationConfigurationName]
     *
     * @param dependency The dependency to add.
     * @param configure Additional configuration for the [dependency].
     * @return The added [dependency].
     */
    fun <T : Dependency> implementation(dependency: T, configure: Action<T>) =
        implementation(dependency) { configure.execute(this) }

    /**
     * Adds a `compileOnly` [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies)
     * to this entity.
     *
     * @see [HasKotlinDependencies.compileOnlyConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun compileOnly(dependencyNotation: Any): Dependency?

    /**
     * Adds a `compileOnly` [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies)
     * to this entity.
     *
     * @see [HasKotlinDependencies.compileOnlyConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @param configure Additional configuration for the created module dependency.
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun compileOnly(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency

    /**
     * Adds a `compileOnly` [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies)
     * to this entity.
     *
     * @see [HasKotlinDependencies.compileOnlyConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @param configure Additional configuration for the created module dependency.
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun compileOnly(dependencyNotation: String, configure: Action<ExternalModuleDependency>) =
        compileOnly(dependencyNotation) { configure.execute(this) }

    /**
     * Adds a `compileOnly` dependency to this entity.
     *
     * @see [HasKotlinDependencies.compileOnlyConfigurationName]
     *
     * @param dependency The dependency to add.
     * @param configure Additional configuration for the [dependency].
     * @return The added [dependency].
     */
    fun <T : Dependency> compileOnly(dependency: T, configure: T.() -> Unit): T

    /**
     * Adds a `compileOnly` dependency to this entity.
     *
     * @see [HasKotlinDependencies.compileOnlyConfigurationName]
     *
     * @param dependency The dependency to add.
     * @param configure Additional configuration for the [dependency].
     * @return The added [dependency].
     */
    fun <T : Dependency> compileOnly(dependency: T, configure: Action<T>) =
        compileOnly(dependency) { configure.execute(this) }

    /**
     * Adds a `runtimeOnly` [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies)
     * to this entity.
     *
     * @see [HasKotlinDependencies.runtimeOnlyConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun runtimeOnly(dependencyNotation: Any): Dependency?

    /**
     * Adds a `runtimeOnly` [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies)
     * to this entity.
     *
     * @see [HasKotlinDependencies.runtimeOnlyConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @param configure Additional configuration for the created module dependency.
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun runtimeOnly(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency

    /**
     * Adds a `runtimeOnly` [module dependency](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sub:module_dependencies)
     * to this entity.
     *
     * @see [HasKotlinDependencies.runtimeOnlyConfigurationName]
     *
     * @param dependencyNotation The module dependency notation, as per [DependencyHandler.create].
     * @param configure Additional configuration for the created module dependency.
     * @return The module dependency, or `null` if dependencyNotation is a provider.
     */
    fun runtimeOnly(dependencyNotation: String, configure: Action<ExternalModuleDependency>) =
        runtimeOnly(dependencyNotation) { configure.execute(this) }

    /**
     * Adds a `runtimeOnly` dependency to this entity.
     *
     * @see [HasKotlinDependencies.runtimeOnlyConfigurationName]
     *
     * @param dependency The dependency to add.
     * @param configure Additional configuration for the [dependency].
     * @return The added [dependency].
     */
    fun <T : Dependency> runtimeOnly(dependency: T, configure: T.() -> Unit): T

    /**
     * Adds a `runtimeOnly` dependency to this entity.
     *
     * @see [HasKotlinDependencies.runtimeOnlyConfigurationName]
     *
     * @param dependency The dependency to add.
     * @param configure Additional configuration for the [dependency].
     * @return The added [dependency].
     */
    fun <T : Dependency> runtimeOnly(dependency: T, configure: Action<T>) =
        runtimeOnly(dependency) { configure.execute(this) }

    /**
     * Creates a dependency to an official Kotlin library with the same version that is configured
     * in [KotlinTopLevelExtensionConfig.coreLibrariesVersion].
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jvmMain"].dependencies {
     *     implementation(kotlin("stdlib"))
     * }
     * ```
     *
     * The official Kotlin dependencies are always part of the "org.jetbrains.kotlin" group and the module name always has prefix: "kotlin-".
     *
     * @param simpleModuleName The Kotlin module name that follows after the "kotlin-" prefix. For example, for "kotlin-reflect":
     * ```
     * implementation(kotlin("reflect"))
     * // equivalent to
     * implementation("org.jetbrains.kotlin:kotlin-reflect")
     * ```
     */
    fun kotlin(simpleModuleName: String): ExternalModuleDependency = kotlin(simpleModuleName, null)

    /**
     * Creates a dependency to an official Kotlin library.
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jvmMain"].dependencies {
     *     implementation(kotlin("stdlib", "2.0.0"))
     * }
     * ```
     *
     * The official Kotlin dependencies are always part of the "org.jetbrains.kotlin" group and the module name always has prefix: "kotlin-".
     *
     * @param simpleModuleName The Kotlin module name followedthat follows after the "kotlin-" prefix. For example, for "kotlin-reflect":
     * ```
     * implementation(kotlin("reflect", "2.0.0"))
     * // equivalent to
     * implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
     * ```
     * @param version dependency version or `null` to use the version defined in [KotlinTopLevelExtensionConfig.coreLibrariesVersion].
     */
    fun kotlin(simpleModuleName: String, version: String?): ExternalModuleDependency

    /**
     * Creates a Gradle project dependency.
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jvmMain"].dependencies {
     *     implementation(project(":my-library", "customLibraryConfiguration"))
     * }
     * ```
     *
     * @param path The project path
     * @param configuration The optional target configuration in the project
     */
    fun project(path: String, configuration: String? = null): ProjectDependency =
        project(listOf("path", "configuration").zip(listOfNotNull(path, configuration)).toMap())

    /**
     * Creates a Gradle project dependency.
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jvmMain"].dependencies {
     *     implementation(project(mapOf("path" to ":project-a", "configuration" to "someOtherConfiguration")))
     * }
     * ```
     *
     * @param notation Project notation described in [DependencyHandler].
     */
    fun project(notation: Map<String, Any?>): ProjectDependency

    /**
     * @suppress
     */
    @Deprecated(
        "Scheduled for removal in Kotlin 2.1. Check KT-58759",
        replaceWith = ReplaceWith("project.dependencies.enforcedPlatform(notation)")
    )
    fun enforcedPlatform(notation: Any): Dependency =
        project.dependencies.enforcedPlatform(notation)

    /**
     * @suppress
     */
    @Deprecated(
        "Scheduled for removal in Kotlin 2.1. Check KT-58759",
        replaceWith = ReplaceWith("project.dependencies.enforcedPlatform(notation, configureAction)")
    )
    fun enforcedPlatform(notation: Any, configureAction: Action<in Dependency>): Dependency =
        project.dependencies.enforcedPlatform(notation, configureAction)

    /**
     * @suppress
     */
    @Deprecated(
        "Scheduled for removal in Kotlin 2.1. Check KT-58759",
        replaceWith = ReplaceWith("project.dependencies.platform(notation)")
    )
    fun platform(notation: Any): Dependency =
        project.dependencies.platform(notation)

    /**
     * @suppress
     */
    @Deprecated(
        "Scheduled for removal in Kotlin 2.1. Check KT-58759",
        replaceWith = ReplaceWith("project.dependencies.platform(notation, configureAction)")
    )
    fun platform(notation: Any, configureAction: Action<in Dependency>): Dependency =
        project.dependencies.platform(notation, configureAction)

    /**
     * @suppress
     */
    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    fun npm(
        name: String,
        version: String,
        generateExternals: Boolean
    ): Dependency {
        @Suppress("deprecation_error")
        (warnNpmGenerateExternals(project.logger))
        return npm(name, version)
    }

    /**
     * Creates a dependency on the [NPM](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#dependencies) module.
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     implementation(npm("is-odd-even", "1.0.0"))
     * }
     * ```
     *
     * This is only relevant for Kotlin entities that target only [KotlinPlatformType.js] or [KotlinPlatformType.wasm].
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     */
    fun npm(
        name: String,
        version: String
    ): Dependency

    /**
     * @suppress
     */
    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    fun npm(
        name: String,
        directory: File,
        generateExternals: Boolean
    ): Dependency {
        @Suppress("deprecation_error")
        (warnNpmGenerateExternals(project.logger))
        return npm(name, directory)
    }

    /**
     * Creates a dependency on the [NPM](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#dependencies) module.
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     implementation(npm("is-odd-even", project.file("npm/is-odd-even")))
     * }
     * ```
     *
     * This is only relevant for Kotlin entities that target only [KotlinPlatformType.js] or [KotlinPlatformType.wasm].
     *
     * @param name The NPM dependency name
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) keyword)
     */
    fun npm(
        name: String,
        directory: File
    ): Dependency

    /**
     * @suppress
     */
    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    fun npm(
        directory: File,
        generateExternals: Boolean
    ): Dependency {
        @Suppress("deprecation_error")
        (warnNpmGenerateExternals(project.logger))
        return npm(directory)
    }

    /**
     * Creates a dependency on the [NPM](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#dependencies) module.
     * The name of the dependency is derived either from the `package.json` file located in the [directory] or the [directory] name itself.
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     implementation(npm(project.file("npm/is-odd-even")))
     * }
     * ```
     *
     * This is only relevant for Kotlin entities that target only [KotlinPlatformType.js] or [KotlinPlatformType.wasm].
     *
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) keyword)
     */
    fun npm(
        directory: File
    ): Dependency

    /**
     * Creates a dependency to a NPM module that is added
     * to [devDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#devdependencies).
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     implementation(devNpm("is-odd-even", "1.1.0"))
     * }
     * ```
     *
     * This is only relevant for Kotlin entities that target only [KotlinPlatformType.js] or [KotlinPlatformType.wasm].
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     */
    fun devNpm(
        name: String,
        version: String
    ): Dependency

    /**
     * Creates a dependency to a NPM module that is added
     * to [devDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#devdependencies).
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     implementation(devNpm("is-odd-even", project.file("npm/is-odd-even")))
     * }
     * ```
     *
     * This is only relevant for Kotlin entities that target only [KotlinPlatformType.js] or [KotlinPlatformType.wasm].
     *
     * @param name The NPM dependency name
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) keyword)
     */
    fun devNpm(
        name: String,
        directory: File
    ): Dependency

    /**
     * Creates a dependency to a NPM module that is added
     * to [devDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#devdependencies).
     * The name of the dependency is derived either from the `package.json` file located in the [directory] or the [directory] name itself.
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     implementation(devNpm(project.file("npm/is-odd-even")))
     * }
     * ```
     *
     * This is only relevant for Kotlin entities that target only [KotlinPlatformType.js] or [KotlinPlatformType.wasm].
     *
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#repository) keyword)
     */
    fun devNpm(
        directory: File
    ): Dependency

    /**
     * @suppress
     */
    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    fun optionalNpm(
        name: String,
        version: String,
        generateExternals: Boolean
    ): Dependency {
        @Suppress("deprecation_error")
        (warnNpmGenerateExternals(project.logger))
        return optionalNpm(name, version)
    }

    /**
     * Creates a dependency to a NPM module that is added
     * to [optionalDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#optionaldependencies).
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     implementation(optionalNpm("is-odd-even", "1.0.0"))
     * }
     * ```
     *
     * This is only relevant for Kotlin entities that target only [KotlinPlatformType.js] or [KotlinPlatformType.wasm].
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     */
    fun optionalNpm(
        name: String,
        version: String
    ): Dependency

    /**
     * @suppress
     */
    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    fun optionalNpm(
        name: String,
        directory: File,
        generateExternals: Boolean
    ): Dependency {
        @Suppress("deprecation_error")
        (warnNpmGenerateExternals(project.logger))
        return optionalNpm(name, directory)
    }

    /**
     * Creates a dependency to a NPM module that is added
     * to [optionalDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#optionaldependencies).
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     implementation(optionalNpm("is-odd-even", project.file("npm/is-odd-even")))
     * }
     * ```
     *
     * **Note**: Only relevant for Kotlin entities targeting only [KotlinPlatformType.js] or [KotlinPlatformType.wasm]!
     *
     * @param name The NPM dependency name
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) keyword)
     */
    fun optionalNpm(
        name: String,
        directory: File
    ): Dependency

    /**
     * @suppress
     */
    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    fun optionalNpm(
        directory: File,
        generateExternals: Boolean
    ): Dependency {
        @Suppress("deprecation_error")
        (warnNpmGenerateExternals(project.logger))
        return optionalNpm(directory)
    }

    /**
     * Creates a dependency to a NPM module that is added
     * to [optionalDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#optionaldependencies).
     * The name of the dependency is derived either from the `package.json` file located in the [directory] or the [directory] name itself.
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     implementation(optionalNpm(project.file("npm/is-odd-even")))
     * }
     * ```
     *
     * This is only relevant for Kotlin entities that target only [KotlinPlatformType.js] or [KotlinPlatformType.wasm].
     *
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) keyword)
     */
    fun optionalNpm(
        directory: File
    ): Dependency

    /**
     * Creates a dependency to a NPM module that is added
     * to [peerDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#peerdependencies).
     *
     * Note: The created dependency should be manually added to this entity using other methods from this DSL:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     implementation(peerNpm("is-odd-even", "1.0.0"))
     * }
     * ```
     *
     * This is only relevant for Kotlin entities that target only [KotlinPlatformType.js] or [KotlinPlatformType.wasm].
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     */
    fun peerNpm(
        name: String,
        version: String
    ): Dependency
}
