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
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.base.classesTaskName
import org.jetbrains.kotlin.gradle.plugin.base.processResourcesTaskName
import org.jetbrains.kotlin.gradle.plugin.executeClosure
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import java.lang.reflect.Constructor

interface KotlinBaseSourceSet : KotlinSourceSet {
    var compileClasspath: FileCollection

    var runtimeClasspath: FileCollection

    val output: SourceSetOutput

    val resources: SourceDirectorySet

    val allSource: SourceDirectorySet

    val classesTaskName: String

    val processResourcesTaskName: String

    val compileKotlinTaskName: String

    val compileConfigurationName: String

    val runtimeConfigurationName: String

    val compileOnlyConfigurationName: String

    val runtimeOnlyConfigurationName: String

    val implementationConfigurationName: String

    val compileClasspathConfigurationName: String

    val runtimeClasspathConfigurationName: String

    fun compiledBy(vararg taskPaths: Any)
}

abstract class AbstractKotlinSourceSet(
    val displayName: String,
    fileResolver: FileResolver
) : KotlinSourceSet {
    override fun getName(): String = displayName

    final override val kotlin: SourceDirectorySet =
        createDefaultSourceDirectorySet(name + " Kotlin source", fileResolver).apply {
            filter.include("**/*.java", "**/*.kt", "**/*.kts")
        }

    final override fun kotlin(configureClosure: Closure<Any?>): SourceDirectorySet {
        ConfigureUtil.configure(configureClosure, kotlin)
        return kotlin
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

class DefaultKotlinSourceSet(
    private val project: Project,
    val displayName: String,
    val fileResolver: FileResolver
) : KotlinSourceSet {
    override val kotlin: SourceDirectorySet = createDefaultSourceDirectorySet(displayName + ".kotlin", fileResolver)

    val resources: SourceDirectorySet = createDefaultSourceDirectorySet(displayName + ".resources", fileResolver)

    override fun kotlin(configureClosure: Closure<Any?>): SourceDirectorySet = kotlin.apply { executeClosure(configureClosure) }

    override fun getName(): String = displayName

    override fun dependencies(configure: KotlinSourceSetDependencyHandler.() -> Unit) {

    }

    private inner class DepenencyHandler : KotlinSourceSetDependencyHandler {
        override fun api(dependencyNotation: Any) {
            TODO("not implemented")
        }

        override fun implementation(dependencyNotation: Any) {
            TODO("not implemented")
        }

        override fun compileOnly(dependencyNotation: Any) {
            TODO("not implemented")
        }

        override fun runtimeOnly(dependencyNotation: Any) {
            TODO("not implemented")
        }

        private fun addDependency(configurationName: String, dependencyNotation: Any) {
            project.dependencies.add(configurationName, dependencyNotation)
        }
    }

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ this@f.executeClosure(configureClosure) }
}

//open class KotlinOnlySourceSet(
//    name: String,
//    fileResolver: FileResolver,
//    newSourceSetOutput: SourceSetOutput,
//    val project: Project,
//) : KotlinSourceSet(name, fileResolver), KotlinBaseSourceSet {
//
//    override fun toString(): String =
//        "source set '$name'" +
//                kotlinTarget.disambiguationClassifier?.let { " ($it)" }.orEmpty()
//
//    override var compileClasspath: FileCollection = project.files()
//
//    override var runtimeClasspath: FileCollection = project.files()
//
//    override val output: SourceSetOutput = newSourceSetOutput
//
//    override val resources: SourceDirectorySet = createDefaultSourceDirectorySet("$name.resources", fileResolver)
//
//    override val allSource: SourceDirectorySet =
//        createDefaultSourceDirectorySet("$name.allSource", fileResolver).apply {
//            source(kotlin)
//            source(resources)
//        }
//
//    override val classesTaskName: String get() = composeName(kotlinTarget.classesTaskName)
//
//    override val jarTaskName: String get() = composeName(suffix = "jar")
//
//    override val processResourcesTaskName: String get() = composeName(kotlinTarget.processResourcesTaskName)
//
//    override val compileKotlinTaskName: String get() = composeName("compile", kotlinTarget.targetName)
//
//    override val compileConfigurationName: String
//        get() = kotlinTarget.disambiguateName(composeName(suffix = "compile"))
//
//    override val runtimeConfigurationName: String
//        get() = kotlinTarget.disambiguateName(composeName(suffix = "runtime"))
//
//    override val compileOnlyConfigurationName: String
//        get() = kotlinTarget.disambiguateName(composeName(suffix = "compileOnly"))
//
//    override val runtimeOnlyConfigurationName: String
//        get() = kotlinTarget.disambiguateName(composeName(suffix = "runtimeOnly"))
//
//    override val implementationConfigurationName: String
//        get() = kotlinTarget.disambiguateName(composeName(suffix = "implementation"))
//
//    override val compileClasspathConfigurationName: String
//        get() = kotlinTarget.disambiguateName(composeName(suffix = "compileClasspath"))
//
//    override val runtimeClasspathConfigurationName: String
//        get() = kotlinTarget.disambiguateName(composeName(suffix = "runtimeClasspath"))
//
//    override fun compiledBy(vararg taskPaths: Any) {
//        (output.classesDirs as ConfigurableFileCollection).from(project.files().builtBy(*taskPaths))
//    }
//}

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