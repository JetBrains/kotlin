/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import java.lang.reflect.Constructor

interface KotlinExtendedSourceSet : KotlinSourceSet {
    var compileClasspath: FileCollection

    var runtimeClasspath: FileCollection

    val output: SourceSetOutput

    val resources: SourceDirectorySet

    val allSource: SourceDirectorySet

    val classesTaskName: String

    val processResourcesTaskName: String

    val compileKotlinTaskName: String

    val jarTaskName: String

    val compileConfigurationName: String

    val runtimeConfigurationName: String

    val compileOnlyConfigurationName: String

    val runtimeOnlyConfigurationName: String

    val implementationConfigurationName: String

    val compileClasspathConfigurationName: String

    val runtimeClasspathConfigurationName: String

    fun compiledBy(vararg taskPaths: Any)

    fun getCompileTaskName(suffix: String): String
}

abstract class AbstractKotlinSourceSet(
    val displayName: String,
    fileResolver: FileResolver,
    val platformName: String
) : KotlinSourceSet {
    override fun getName(): String = displayName

    final override val kotlin: SourceDirectorySet =
        createDefaultSourceDirectorySet(name + " Kotlin source", fileResolver).apply {
            filter.include("**/*.java", "**/*.kt", "**/*.kts")
        }

    final override fun kotlin(configureClosure: Closure<Any?>?): KotlinSourceSet {
        ConfigureUtil.configure(configureClosure, kotlin)
        return this
    }
}

class KotlinJavaSourceSet(
    displayName: String,
    fileResolver: FileResolver,
    val javaSourceSet: SourceSet
) : AbstractKotlinSourceSet(displayName, fileResolver, ""), KotlinExtendedSourceSet {
    override fun getCompileTaskName(suffix: String): String = composeName("compile", "Kotlin")

    val java: SourceDirectorySet get() = javaSourceSet.java

    val allJava: SourceDirectorySet get() = javaSourceSet.allJava

    val compileJavaTaskName: String get() = javaSourceSet.compileJavaTaskName

    override var compileClasspath: FileCollection
        get() = javaSourceSet.compileClasspath
        set(value) {
            javaSourceSet.compileClasspath = value
        }

    override var runtimeClasspath: FileCollection
        get() = javaSourceSet.runtimeClasspath
        set(value) {
            javaSourceSet.runtimeClasspath = value
        }

    override val output: SourceSetOutput get() = javaSourceSet.output

    override val resources: SourceDirectorySet get() = javaSourceSet.resources

    override val allSource: SourceDirectorySet get() = javaSourceSet.allSource

    override val compileKotlinTaskName: String get() = javaSourceSet.getTaskName("compile", "Kotlin")

    override val classesTaskName: String get() = javaSourceSet.classesTaskName

    override val jarTaskName: String get() = javaSourceSet.jarTaskName

    override val processResourcesTaskName: String get() = javaSourceSet.processResourcesTaskName

    override val compileConfigurationName: String get() = javaSourceSet.compileConfigurationName

    override val runtimeConfigurationName: String get() = javaSourceSet.runtimeConfigurationName

    override val compileOnlyConfigurationName: String get() = javaSourceSet.compileOnlyConfigurationName

    override val runtimeOnlyConfigurationName: String get() = javaSourceSet.runtimeOnlyConfigurationName

    override val implementationConfigurationName: String get() = javaSourceSet.implementationConfigurationName

    override val compileClasspathConfigurationName: String get() = javaSourceSet.compileClasspathConfigurationName

    override val runtimeClasspathConfigurationName: String get() = javaSourceSet.runtimeClasspathConfigurationName

    override fun compiledBy(vararg taskPaths: Any) {
        javaSourceSet.compiledBy(*taskPaths)
    }
}

internal fun KotlinSourceSet.composeName(prefix: String? = null, suffix: String? = null): String {
    val sourceSetName = (if (name == "main") "" else name).let {
        if (prefix.isNullOrEmpty()) it else it.capitalize()
    }
    val resultPrefix = (prefix ?: "") + sourceSetName
    val resultSuffix = (if (resultPrefix.isEmpty()) suffix else suffix?.capitalize()) ?: ""
    return resultPrefix + resultSuffix
}

open class KotlinOnlySourceSet(
    name: String,
    fileResolver: FileResolver,
    newSourceSetOutput: SourceSetOutput,
    val project: Project,
    platformName: String = "kotlin"
) : AbstractKotlinSourceSet(name, fileResolver, platformName), KotlinExtendedSourceSet {

    override fun getCompileTaskName(suffix: String): String = composeName("compile", suffix)

    override var compileClasspath: FileCollection = project.files()

    override var runtimeClasspath: FileCollection = project.files()

    override val output: SourceSetOutput = newSourceSetOutput

    override val resources: SourceDirectorySet = createDefaultSourceDirectorySet("$name.resources", fileResolver)

    override val allSource: SourceDirectorySet = createDefaultSourceDirectorySet("$name.allSource", fileResolver)

    override val classesTaskName: String get() = composeName(suffix = "classes")

    override val jarTaskName: String get() = composeName(suffix = "jar")

    override val processResourcesTaskName: String get() = composeName("process", "resources")

    override val compileKotlinTaskName: String get() = composeName("compile", platformName)

    override val compileConfigurationName: String get() = composeName(suffix = "compile")

    override val runtimeConfigurationName: String get() = composeName(suffix = "runtime")

    override val compileOnlyConfigurationName: String get() = composeName(suffix = "compileOnly")

    override val runtimeOnlyConfigurationName: String get() = composeName(suffix = "runtimeOnly")

    override val implementationConfigurationName: String get() = composeName(suffix = "implementation")

    override val compileClasspathConfigurationName: String get() = composeName(suffix = "compileClasspath")

    override val runtimeClasspathConfigurationName: String get() = composeName(suffix = "runtimeClasspath")

    override fun compiledBy(vararg taskPaths: Any) {
        (output.classesDirs as ConfigurableFileCollection).from(project.files().builtBy(*taskPaths))
    }
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

private fun <T> Class<T>.constructorOrNull(vararg parameterTypes: Class<*>): Constructor<T>? =
    try {
        getConstructor(*parameterTypes)
    } catch (e: NoSuchMethodException) {
        null
    }