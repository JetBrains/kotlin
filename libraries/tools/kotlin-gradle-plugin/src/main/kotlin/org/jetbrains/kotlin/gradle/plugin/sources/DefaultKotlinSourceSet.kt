/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import groovy.lang.Closure
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.lang.reflect.Constructor
import java.util.*

const val METADATA_CONFIGURATION_NAME_SUFFIX = "DependenciesMetadata"

class DefaultKotlinSourceSet(
    private val project: Project,
    val displayName: String,
    fileResolver: FileResolver
) : KotlinSourceSet {

    override val apiConfigurationName: String
        get() = disambiguateName("api")

    override val implementationConfigurationName: String
        get() = disambiguateName("implementation")

    override val compileOnlyConfigurationName: String
        get() = disambiguateName("compileOnly")

    override val runtimeOnlyConfigurationName: String
        get() = disambiguateName("runtimeOnly")

    override val apiMetadataConfigurationName: String
        get() = lowerCamelCaseName(apiConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val implementationMetadataConfigurationName: String
        get() = lowerCamelCaseName(implementationConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val compileOnlyMetadataConfigurationName: String
        get() = lowerCamelCaseName(compileOnlyConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val runtimeOnlyMetadataConfigurationName: String
        get() = lowerCamelCaseName(runtimeOnlyConfigurationName, METADATA_CONFIGURATION_NAME_SUFFIX)

    override val kotlin: SourceDirectorySet = createDefaultSourceDirectorySet(name + " Kotlin source", fileResolver).apply {
        filter.include("**/*.java", "**/*.kt", "**/*.kts")
    }

    override val languageSettings: LanguageSettingsBuilder = DefaultLanguageSettingsBuilder()

    override val resources: SourceDirectorySet = createDefaultSourceDirectorySet(displayName + " resources", fileResolver)

    override fun kotlin(configureClosure: Closure<Any?>): SourceDirectorySet = kotlin.apply { ConfigureUtil.configure(configureClosure, this) }

    override fun languageSettings(configureClosure: Closure<Any?>): LanguageSettingsBuilder = languageSettings.apply {
        ConfigureUtil.configure(configureClosure, this)
    }

    override fun getName(): String = displayName

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, project).run(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ ConfigureUtil.configure(configureClosure, this@f) }

    override fun dependsOn(other: KotlinSourceSet) {
        dependsOnSourceSetsImpl.add(other)

        // Fail-fast approach: check on each new added edge and report a circular dependency at once when the edge is added.
        checkForCircularDependencies()

        project.afterEvaluate { defaultSourceSetLanguageSettingsChecker.runAllChecks(this, other) }
    }

    private val dependsOnSourceSetsImpl = mutableSetOf<KotlinSourceSet>()

    override val dependsOn: Set<KotlinSourceSet>
        get() = dependsOnSourceSetsImpl

    override fun toString(): String = "source set $name"
}

private fun KotlinSourceSet.checkForCircularDependencies(): Unit {
    // If adding an edge creates a cycle, than the source node of the edge belongs to the cycle, so run DFS from that node
    // to check whether it became reachable from itself
    val visited = hashSetOf<KotlinSourceSet>()
    val stack = LinkedHashSet<KotlinSourceSet>() // Store the stack explicitly to pretty-print the cycle

    fun checkReachableRecursively(from: KotlinSourceSet) {
        stack += from
        visited += from

        for (to in from.dependsOn) {
            if (to == this@checkForCircularDependencies)
                throw InvalidUserCodeException(
                    "Circular dependsOn hierarchy found in the Kotlin source sets: " +
                            (stack.toList() + to).joinToString(" -> ") { it.name }
                )

            if (to !in visited) {
                checkReachableRecursively(to)
            }
        }
        stack -= from
    }

    checkReachableRecursively(this@checkForCircularDependencies)
}

internal fun KotlinSourceSet.disambiguateName(simpleName: String): String {
    val nameParts = listOfNotNull(this.name.takeIf { it != "main" }, simpleName)
    return lowerCamelCaseName(*nameParts.toTypedArray())
}

private val createDefaultSourceDirectorySet: (name: String?, resolver: FileResolver?) -> SourceDirectorySet = run {
    val klass = DefaultSourceDirectorySet::class.java
    val defaultConstructor = klass.constructorOrNull(String::class.java, FileResolver::class.java)

    if (defaultConstructor != null && defaultConstructor.getAnnotation(java.lang.Deprecated::class.java) == null) {
        // TODO: drop when gradle < 2.12 are obsolete
        { name, resolver -> defaultConstructor.newInstance(name, resolver) }
    } else {
        val directoryFileTreeFactoryClass = Class.forName("org.gradle.api.internal.file.collections.DirectoryFileTreeFactory")
        val alternativeConstructor = klass.getConstructor(String::class.java, FileResolver::class.java, directoryFileTreeFactoryClass)

        val defaultFileTreeFactoryClass = Class.forName("org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory")
        val defaultFileTreeFactory = defaultFileTreeFactoryClass.getConstructor().newInstance()
        return@run { name, resolver -> alternativeConstructor.newInstance(name, resolver, defaultFileTreeFactory) }
    }
}

internal fun KotlinSourceSet.getSourceSetHierarchy(): Set<KotlinSourceSet> {
    val result = mutableSetOf<KotlinSourceSet>()

    fun processSourceSet(sourceSet: KotlinSourceSet) {
        if (result.add(sourceSet)) {
            sourceSet.dependsOn.forEach { processSourceSet(it) }
        }
    }

    processSourceSet(this)
    return result
}


private fun <T> Class<T>.constructorOrNull(vararg parameterTypes: Class<*>): Constructor<T>? =
    try {
        getConstructor(*parameterTypes)
    } catch (e: NoSuchMethodException) {
        null
    }