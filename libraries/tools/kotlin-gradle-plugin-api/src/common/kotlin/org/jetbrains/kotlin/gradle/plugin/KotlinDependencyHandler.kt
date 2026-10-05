/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
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
     * Creates a dependency to an official Kotlin library with the same version that is configured in
     * [org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension.coreLibrariesVersion].
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
     * @param version dependency version or `null` to use the version defined in
     * [org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension.coreLibrariesVersion].
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
     *     implementation(project(":my-library"))
     * }
     * ```
     *
     * @param path The project path
     */
    fun project(path: String) = project(path, null)

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
     * Declares a dependency on the [NPM](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#dependencies) module.
     *
     * Calling [npm] automatically adds the dependency to the enclosing dependency collector:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npm("is-odd-even", "1.0.0")
     * }
     * ```
     *
     * NPM dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Passing the returned dependency to them, as in `implementation(npm("is-odd-even", "1.0.0"))`, is deprecated -
     * the returned value is only kept for backward compatibility.
     *
     * The version will be parsed by node-semver.
     * See [the node-semver README](https://github.com/npm/node-semver/tree/v7.8.5#versions) for the supported syntax.
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     * @return The declared NPM dependency, kept for backward compatibility; it must not be passed to a Gradle dependency scope.
     */
    fun npm(
        name: String,
        version: String,
    ): Dependency

    /**
     * Declares a dependency on the [NPM](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#dependencies) module
     * located in a local [directory].
     *
     * Calling [npm] automatically adds the dependency to the enclosing dependency collector:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npm("is-odd-even", project.file("npm/is-odd-even"))
     * }
     * ```
     *
     * NPM dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Passing the returned dependency to them, as in `implementation(npm("is-odd-even", project.file("npm/is-odd-even")))`,
     * is deprecated - the returned value is only kept for backward compatibility.
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name.
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) keyword)
     * @return The declared NPM dependency, kept for backward compatibility; it must not be passed to a Gradle dependency scope.
     */
    fun npm(
        name: String,
        directory: File,
    ): Dependency

    /**
     * **Deprecated** - deriving the NPM dependency name from the [directory] is deprecated.
     * Declare the name explicitly instead:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npm("is-odd-even", project.file("npm/is-odd-even"))
     * }
     * ```
     *
     * Scheduled for removal in Kotlin 2.7.
     *
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) keyword)
     * @return The declared NPM dependency, kept for backward compatibility; it must not be passed to a Gradle dependency scope.
     */
    @Deprecated(
        message = "Deriving the NPM dependency name from the directory is deprecated, declare the name explicitly",
        replaceWith = ReplaceWith("""npm("dependency-name", directory)"""),
        level = DeprecationLevel.WARNING,
    )
    fun npm(
        directory: File,
    ): Dependency

    /**
     * Declares a dependency on a NPM module that is added to [devDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#devdependencies).
     *
     * NPM dev dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Calling [npmDev] will automatically add it to the enclosing dependency collector.
     *
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmDev("is-odd-even", "1.1.0")
     * }
     * ```
     *
     * The version will be parsed by node-semver.
     * See [the node-semver README](https://github.com/npm/node-semver/tree/v7.8.5#versions) for the supported syntax.
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     */
    fun npmDev(
        name: String,
        version: String,
    )

    /**
     * Declares a dependency on a NPM module that is added to [devDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#devdependencies).
     *
     * NPM dev dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Calling [npmDev] will automatically add it to the enclosing dependency collector.
     *
     * [version] is resolved lazily, so it may be provided by a value
     * that is not yet known when the declaration is made:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmDev(
     *         "is-odd-even",
     *         project.providers.gradleProperty("isOddEvenVersion"),
     *     )
     * }
     * ```
     *
     * If [version] has no value, then the build fails when the declaration is used.
     *
     * The version will be parsed by node-semver.
     * See [the node-semver README](https://github.com/npm/node-semver/tree/v7.8.5#versions) for the supported syntax.
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param version The provider of the NPM dependency version
     */
    fun npmDev(
        name: String,
        version: Provider<String>,
    )

    /**
     * Declares a dependency on a NPM module located in a local [directory]
     * that is added to [devDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#devdependencies).
     *
     * NPM dev dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Calling [npmDev] will automatically add it to the enclosing dependency collector.
     *
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmDev("is-odd-even", project.file("npm/is-odd-even"))
     * }
     * ```
     *
     * The dependency is declared with the NPM
     * [local path](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) notation
     * using the absolute path of the [directory].
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param directory The directory where dependency files are located
     */
    fun npmDev(
        name: String,
        directory: File,
    )

    /**
     * Declares a dependency on a NPM module located in a local [directory]
     * that is added to [devDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#devdependencies).
     *
     * NPM dev dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Calling [npmDevDirectory] will automatically add it to the enclosing dependency collector.
     *
     * [directory] is resolved lazily, so it may be provided by a value
     * that are not yet known when the declaration is made:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmDev("is-odd-even", project.layout.buildDirectory.dir("npm/is-odd-even"))
     * }
     * ```
     *
     * If [directory] has no value, then the build fails when the declaration is used.
     *
     * The dependency is declared with the NPM
     * [local path](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) notation
     * using the absolute path of the [directory].
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param directory The provider of the directory where dependency files are located
     */
    @ExperimentalKotlinGradlePluginApi
    fun npmDevDirectory(
        name: String,
        directory: Provider<Directory>,
    )

    /**
     * Declares a dependency on a NPM module that is added to [optionalDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#optionaldependencies).
     *
     * NPM optional dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Calling [npmOptional] will automatically add it to the enclosing dependency collector.
     *
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmOptional("is-odd-even", "1.1.0")
     * }
     * ```
     *
     * The version will be parsed by node-semver.
     * See [the node-semver README](https://github.com/npm/node-semver/tree/v7.8.5#versions) for the supported syntax.
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     */
    fun npmOptional(
        name: String,
        version: String,
    )

    /**
     * Declares a dependency on a NPM module that is added to [optionalDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#optionaldependencies).
     *
     * NPM optional dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Calling [npmOptional] will automatically add it to the enclosing dependency collector.
     *
     * [version] is resolved lazily, so it may be provided by a value
     * that is not yet known when the declaration is made:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmOptional(
     *         "is-odd-even",
     *         project.providers.gradleProperty("isOddEvenVersion"),
     *     )
     * }
     * ```
     *
     * If [version] has no value, then the build fails when the declaration is used.
     *
     * The version will be parsed by node-semver.
     * See [the node-semver README](https://github.com/npm/node-semver/tree/v7.8.5#versions) for the supported syntax.
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param version The provider of the NPM dependency version
     */
    fun npmOptional(
        name: String,
        version: Provider<String>,
    )

    /**
     * Declares a dependency on a NPM module located in a local [directory]
     * that is added to [optionalDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#optionaldependencies).
     *
     * NPM optional dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Calling [npmOptional] will automatically add it to the enclosing dependency collector.
     *
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmOptional("is-odd-even", project.file("npm/is-odd-even"))
     * }
     * ```
     *
     * The dependency is declared with the NPM
     * [local path](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) notation
     * using the absolute path of the [directory].
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param directory The directory where dependency files are located
     */
    fun npmOptional(
        name: String,
        directory: File,
    )

    /**
     * Declares a dependency on a NPM module located in a local [directory]
     * that is added to [optionalDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#optionaldependencies).
     *
     * NPM optional dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Calling [npmOptionalDirectory] will automatically add it to the enclosing dependency collector.
     *
     * [directory] is resolved lazily, so it may be provided by a value
     * that are not yet known when the declaration is made:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmOptional("is-odd-even", project.layout.buildDirectory.dir("npm/is-odd-even"))
     * }
     * ```
     *
     * If [directory] has no value, then the build fails when the declaration is used.
     *
     * The dependency is declared with the NPM
     * [local path](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) notation
     * using the absolute path of the [directory].
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param directory The provider of the directory where dependency files are located
     */
    @ExperimentalKotlinGradlePluginApi
    fun npmOptionalDirectory(
        name: String,
        directory: Provider<Directory>,
    )

    /**
     * Declares a dependency on a NPM module that is added to [peerDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#peerdependencies).
     *
     * NPM peer dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Calling [npmPeer] will automatically add it to the enclosing dependency collector.
     *
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmPeer("is-odd-even", "1.1.0")
     * }
     * ```
     *
     * The version will be parsed by node-semver.
     * See [the node-semver README](https://github.com/npm/node-semver/tree/v7.8.5#versions) for the supported syntax.
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     */
    fun npmPeer(
        name: String,
        version: String,
    )

    /**
     * Declares a dependency on a NPM module that is added to [peerDependencies](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#peerdependencies).
     *
     * NPM peer dependencies should not be applied to Gradle dependency scopes such as [api], [implementation], and so on.
     * Calling [npmPeer] will automatically add it to the enclosing dependency collector.
     *
     * [version] is resolved lazily, so it may be provided by a value
     * that is not yet known when the declaration is made:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmPeer(
     *         "is-odd-even",
     *         project.providers.gradleProperty("isOddEvenVersion"),
     *     )
     * }
     * ```
     *
     * If [version] has no value, then the build fails when the declaration is used.
     *
     * The version will be parsed by node-semver.
     * See [the node-semver README](https://github.com/npm/node-semver/tree/v7.8.5#versions) for the supported syntax.
     *
     * Creating NPM dependencies is only relevant for Kotlin entities that target JS or WasmJS.
     *
     * @param name The NPM dependency name
     * @param version The provider of the NPM dependency version
     */
    fun npmPeer(
        name: String,
        version: Provider<String>,
    )

    /**
     * **Deprecated** - wrapping npm dependencies in `implementation(devNpm(...))` is deprecated.
     * Declare them with [npmDev] instead, as a standalone statement:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmDev("is-odd-even", "1.1.0")
     * }
     * ```
     *
     * Scheduled for removal in Kotlin 2.7.
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     */
    @Deprecated(
        message = "Deprecated in favor of npmDev",
        replaceWith = ReplaceWith("npmDev(name, version) /* must not be passed to the implementation(...) or api(...) calls */"),
        level = DeprecationLevel.WARNING,
    )
    fun devNpm(
        name: String,
        version: String,
    ): Dependency

    /**
     * **Deprecated** - wrapping npm dependencies in `implementation(devNpm(...))` is deprecated.
     * Declare them with [npmDev] instead, as a standalone statement:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmDev("is-odd-even", project.file("npm/is-odd-even"))
     * }
     * ```
     *
     * Scheduled for removal in Kotlin 2.7.
     *
     * @param name The NPM dependency name
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) keyword)
     */
    @Deprecated(
        message = "Deprecated in favor of npmDev",
        replaceWith = ReplaceWith("npmDev(name, directory) /* must not be passed to the implementation(...) or api(...) calls */"),
        level = DeprecationLevel.WARNING,
    )
    fun devNpm(
        name: String,
        directory: File,
    ): Dependency

    /**
     * **Deprecated** - wrapping npm dependencies in `implementation(devNpm(...))` is deprecated.
     * Declare them with [npmDev] instead, as a standalone statement with an explicit dependency name:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmDev("is-odd-even", project.file("npm/is-odd-even"))
     * }
     * ```
     *
     * Scheduled for removal in Kotlin 2.7.
     *
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#repository) keyword)
     */
    @Deprecated(
        message = "Deprecated in favor of npmDev",
        replaceWith = ReplaceWith("""npmDev("dependency-name", directory) /* must not be passed to the implementation(...) or api(...) calls */"""),
        level = DeprecationLevel.WARNING,
    )
    fun devNpm(
        directory: File,
    ): Dependency

    /**
     * **Deprecated** - wrapping npm dependencies in `implementation(optionalNpm(...))` is deprecated.
     * Declare them with [npmOptional] instead, as a standalone statement:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmOptional("is-odd-even", "1.1.0")
     * }
     * ```
     *
     * Scheduled for removal in Kotlin 2.7.
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     */
    @Deprecated(
        message = "Deprecated in favor of npmOptional",
        replaceWith = ReplaceWith("npmOptional(name, version) /* must not be passed to the implementation(...) or api(...) calls */"),
        level = DeprecationLevel.WARNING,
    )
    fun optionalNpm(
        name: String,
        version: String,
    ): Dependency

    /**
     * **Deprecated** - wrapping npm dependencies in `implementation(optionalNpm(...))` is deprecated.
     * Declare them with [npmOptional] instead, as a standalone statement:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmOptional("is-odd-even", project.file("npm/is-odd-even"))
     * }
     * ```
     *
     * Scheduled for removal in Kotlin 2.7.
     *
     * @param name The NPM dependency name
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) keyword)
     */
    @Deprecated(
        message = "Deprecated in favor of npmOptional",
        replaceWith = ReplaceWith("npmOptional(name, directory) /* must not be passed to the implementation(...) or api(...) calls */"),
        level = DeprecationLevel.WARNING,
    )
    fun optionalNpm(
        name: String,
        directory: File,
    ): Dependency

    /**
     * **Deprecated** - wrapping npm dependencies in `implementation(optionalNpm(...))` is deprecated.
     * Declare them with [npmOptional] instead, as a standalone statement with an explicit dependency name:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmOptional("is-odd-even", project.file("npm/is-odd-even"))
     * }
     * ```
     *
     * Scheduled for removal in Kotlin 2.7.
     *
     * @param directory The directory where dependency files are located
     * (See NPM [directory](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#local-paths) keyword)
     */
    @Deprecated(
        message = "Deprecated in favor of npmOptional",
        replaceWith = ReplaceWith("""npmOptional("dependency-name", directory) /* must not be passed to the implementation(...) or api(...) calls */"""),
        level = DeprecationLevel.WARNING,
    )
    fun optionalNpm(
        directory: File,
    ): Dependency

    /**
     * **Deprecated** - wrapping npm dependencies in `implementation(peerNpm(...))` is deprecated.
     * Declare them with [npmPeer] instead, as a standalone statement:
     * ```
     * kotlin.sourceSets["jsMain"].dependencies {
     *     npmPeer("is-odd-even", "1.1.0")
     * }
     * ```
     *
     * Scheduled for removal in Kotlin 2.7.
     *
     * @param name The NPM dependency name
     * @param version The NPM dependency version
     */
    @Deprecated(
        message = "Deprecated in favor of npmPeer",
        replaceWith = ReplaceWith("npmPeer(name, version) /* must not be passed to the implementation(...) or api(...) calls */"),
        level = DeprecationLevel.WARNING,
    )
    fun peerNpm(
        name: String,
        version: String,
    ): Dependency
}
