/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dokkabuild.utils

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*


/**
 * Mark this [Configuration] as one that should be used to declare dependencies in
 * [org.gradle.api.Project.dependencies] block.
 *
 * Declarable Configurations should be extended by [resolvable] and [consumable] Configurations.
 * They must not have attributes.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = false
 * isCanBeDeclared = true
 * ```
 */
internal fun Configuration.declarable(
    visible: Boolean = false,
) {
    isCanBeResolved = false
    isCanBeConsumed = false
    @Suppress("UnstableApiUsage")
    isCanBeDeclared = true
    isVisible = visible
}


/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * Consumable Configurations must extend a [declarable] Configuration.
 * They should have attributes.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.consumable(
    visible: Boolean = false,
) {
    isCanBeResolved = false
    isCanBeConsumed = true
    @Suppress("UnstableApiUsage")
    isCanBeDeclared = false
    isVisible = visible
}


/**
 * Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving')
 *
 * Resolvable Configurations should have attributes.
 *
 * ```
 * isCanBeResolved = true
 * isCanBeConsumed = false
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.resolvable(
    visible: Boolean = false,
) {
    isCanBeResolved = true
    isCanBeConsumed = false
    @Suppress("UnstableApiUsage")
    isCanBeDeclared = false
    isVisible = visible
}


internal fun AttributeContainer.jvmJar(objects: ObjectFactory) {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
}

/**
 * Pretty-print the Java name.
 *
 * For Java 8 and below the version is prefixed with `1.`.
 */
fun JavaLanguageVersion.formattedName(): String =
    if (asInt() <= 8) "1.${asInt()}" else asInt().toString()

/**
 * Disable publishing of test fixtures (which causes warnings when publishing).
 *
 * https://docs.gradle.org/current/userguide/java_testing.html#publishing_test_fixtures
 */
fun Project.skipTestFixturesPublications() {
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
    javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
}

/**
 * Exclude embedded Gradle dependencies from the given [SourceSet] configurations.
 *
 * The excluded dependencies are embedded into Gradle, they so should be excluded to prevent
 * classpath ordering issues.
 */
// After extensive manual testing, it appears that these exclusions have no effect in dokka-gradle-plugin.
//
// DGP has no direct runtime dependencies on any of the conflicting dependencies, and Gradle has
// constraints that force the transitive dependencies to match the embedded versions.
// Unfortunately this means there is no way of testing if this config works, or can be safely removed.
// It was originally added because in previous versions DGP did have a direct dependency on
// kotlin-stdlib, but the buildscript has been re-written to correct this, meaning there are no
// longer conflicting dependencies.
//
// These exclusions have been kept due to an abundance of caution.
//
// See also:
// - https://youtrack.jetbrains.com/issue/KT-41142
// - https://github.com/JetBrains/kotlin/blob/2f41a05651e4709fcb6984bbac769af8e8f63935/libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/SimpleKotlinGradleIT.kt#L180
// - https://github.com/Kotlin/dokka/pull/2570
fun Project.excludeGradleEmbeddedDependencies(sourceSet: NamedDomainObjectProvider<SourceSet>) {
    val excludeAction = Action<Configuration> {
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

    sourceSet.configure {
        configurations.named(implementationConfigurationName, excludeAction)
        configurations.named(apiConfigurationName, excludeAction)
        configurations.named(runtimeOnlyConfigurationName, excludeAction)
    }
}
