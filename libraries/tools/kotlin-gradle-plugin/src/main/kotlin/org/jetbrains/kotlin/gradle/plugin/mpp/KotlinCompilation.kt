/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal fun KotlinCompilation.composeName(prefix: String? = null, suffix: String? = null): String {
    val compilationNamePart = compilationName.takeIf { it != "main" }
    val targetNamePart = target.disambiguationClassifier

    return lowerCamelCaseName(prefix, targetNamePart, compilationNamePart, suffix)
}

internal val KotlinCompilation.fullName: String
    get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName)

internal class DefaultKotlinDependencyHandler(
    val parent: HasKotlinDependencies,
    val project: Project
) : KotlinDependencyHandler {
    override fun api(dependencyNotation: Any) = addDependency(parent.apiConfigurationName, dependencyNotation)

    override fun implementation(dependencyNotation: Any) = addDependency(parent.implementationConfigurationName, dependencyNotation)

    override fun compileOnly(dependencyNotation: Any) = addDependency(parent.compileOnlyConfigurationName, dependencyNotation)

    override fun runtimeOnly(dependencyNotation: Any) = addDependency(parent.runtimeOnlyConfigurationName, dependencyNotation)

    private fun addDependency(configurationName: String, dependencyNotation: Any) {
        project.dependencies.add(configurationName, dependencyNotation)
    }
}

abstract class AbstractKotlinCompilation(
    final override val target: KotlinTarget,
    override val compilationName: String
) : KotlinCompilation, HasKotlinDependencies {
    private val attributeContainer = HierarchyAttributeContainer(target.attributes)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val kotlinSourceSets: MutableList<KotlinSourceSet> = mutableListOf()

    override fun source(sourceSet: KotlinSourceSet) {
        kotlinSourceSets += sourceSet

        with(target.project) {
            whenEvaluated {
                (target.project.tasks.getByName(compileKotlinTaskName) as AbstractKotlinCompile<*>).source(sourceSet.kotlin)
            }
            addExtendsFromRelation(apiConfigurationName, sourceSet.apiConfigurationName)
            addExtendsFromRelation(implementationConfigurationName, sourceSet.implementationConfigurationName)
            addExtendsFromRelation(compileOnlyConfigurationName, sourceSet.compileOnlyConfigurationName)
            if (this is KotlinCompilationToRunnableFiles) {
                addExtendsFromRelation(runtimeOnlyConfigurationName, sourceSet.runtimeOnlyConfigurationName)
            }
        }
    }

    protected fun Project.addExtendsFromRelation(extendingConfigurationName: String, extendsFromConfigurationName: String) {
        if (extendingConfigurationName != extendsFromConfigurationName) {
            if (project.configurations.findByName(extendingConfigurationName) != null)
                project.dependencies.add(extendingConfigurationName, project.configurations.getByName(extendsFromConfigurationName))
        }
    }

    override val compileDependencyConfigurationName: String
        get() = lowerCamelCaseName(compilationName.takeIf { it != "main" }.orEmpty(), target.disambiguationClassifier, "compileClasspath")

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName("compile", compilationName.takeIf { it != "main" }.orEmpty(), "Kotlin", target.disambiguationClassifier)

    override val compileAllTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName.takeIf { it != "main" }.orEmpty(), "classes")

    override lateinit var compileDependencyFiles: FileCollection

    override val apiConfigurationName: String
        get() = disambiguateName("api")

    override val implementationConfigurationName: String
        get() = disambiguateName("implementation")

    override val compileOnlyConfigurationName: String
        get() = disambiguateName("compileOnly")

    override val runtimeOnlyConfigurationName: String
        get() = disambiguateName("runtimeOnly")

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, target.project).run(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ this@f.executeClosure(configureClosure) }

    override fun toString(): String = "compilation '$compilationName' ($target)"
}

abstract class AbstractKotlinCompilationToRunnableFiles(target: KotlinTarget, name: String)
    : AbstractKotlinCompilation(target, name), KotlinCompilationToRunnableFiles {
    override val runtimeDependencyConfigurationName: String
        get() = lowerCamelCaseName(compilationName, target.disambiguationClassifier, "runtimeClasspath")

    override lateinit var runtimeDependencyFiles: FileCollection
}

internal fun KotlinCompilation.disambiguateName(simpleName: String): String {
    return lowerCamelCaseName(
        target.disambiguationClassifier,
        compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
        simpleName
    )
}

open class KotlinJvmCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilationToRunnableFiles(target, name), KotlinCompilationWithResources {
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")
}

class KotlinWithJavaCompilation(
    target: KotlinWithJavaTarget,
    name: String,
    val javaSourceSet: SourceSet
) : AbstractKotlinCompilationToRunnableFiles(target, name), KotlinCompilationWithResources {
    override val output: SourceSetOutput
        get() = javaSourceSet.output

    override val processResourcesTaskName: String
        get() = javaSourceSet.processResourcesTaskName

    override var runtimeDependencyFiles: FileCollection
        get() = javaSourceSet.runtimeClasspath
        set(value) { javaSourceSet.runtimeClasspath = value }

    override val runtimeDependencyConfigurationName: String
        get() = javaSourceSet.runtimeClasspathConfigurationName

    override val compileDependencyConfigurationName: String
        get() = javaSourceSet.compileClasspathConfigurationName

    override val runtimeOnlyConfigurationName: String
        get() = javaSourceSet.runtimeOnlyConfigurationName

    override val implementationConfigurationName: String
        get() = javaSourceSet.implementationConfigurationName

    override val apiConfigurationName: String
        get() = javaSourceSet.apiConfigurationName

    override val compileOnlyConfigurationName: String
        get() = javaSourceSet.compileOnlyConfigurationName

    override val compileAllTaskName: String
        get() = javaSourceSet.classesTaskName

    override var compileDependencyFiles: FileCollection
        get() = javaSourceSet.compileClasspath
        set(value) { javaSourceSet.compileClasspath = value }

    fun source(javaSourceSet: SourceSet) {
        with(target.project) {
            afterEvaluate {
                (tasks.getByName(compileKotlinTaskName) as AbstractKotlinCompile<*>).source(javaSourceSet.java)
            }
        }
    }
}

class KotlinJvmAndroidCompilation(
    target: KotlinAndroidTarget,
    name: String,
    override val output: SourceSetOutput
): AbstractKotlinCompilation(target, name)

class KotlinJsCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilation(target, name)

class KotlinCommonCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilation(target, name)