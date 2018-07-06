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

    override val kotlinSourceSets: MutableList<KotlinSourceSet>
        get() = mutableListOf()

    override fun source(sourceSet: KotlinSourceSet) {
        kotlinSourceSets += sourceSet

        with(target.project) {
            // TODO: do this in a more lazy and flexible way
            afterEvaluate {
                (target.project.tasks.findByName(compileKotlinTaskName) as AbstractKotlinCompile<*>).source(sourceSet.kotlin)
            }
            addExtendsFromRelation(apiConfigurationName, sourceSet.apiConfigurationName)
            addExtendsFromRelation(implementationConfigurationName, sourceSet.implementationConfigurationName)
            addExtendsFromRelation(compileOnlyConfigurationName, sourceSet.compileOnlyConfigurationName)
            if (this is KotlinCompilationToRunnableFiles) {
                addExtendsFromRelation(runtimeOnlyConfigurationName, sourceSet.runtimeOnlyConfigurationName)
            }
        }
    }

    private fun Project.addExtendsFromRelation(extendingConfigurationName: String, extendsFromConfigurationName: String) {
        if (extendingConfigurationName != extendsFromConfigurationName) {
            project.dependencies.add(extendingConfigurationName, project.configurations.getByName(extendsFromConfigurationName))
        }
    }

    override val compileDependencyConfigurationName: String
        get() = lowerCamelCaseName(compilationName.takeIf { it != "main" }.orEmpty(), target.disambiguationClassifier, "compileClasspath")

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName("compile", compilationName.takeIf { it != "main" }.orEmpty(), "Kotlin", target.targetName)

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

class KotlinJvmWithJavaCompilation(
    target: KotlinWithJavaTarget,
    name: String,
    val javaSourceSet: SourceSet
) : KotlinJvmCompilation(target, name, javaSourceSet.output) {
    override val output: SourceSetOutput
        get() = javaSourceSet.output
}

class KotlinJvmAndroidCompilation(
    target: KotlinAndroidTarget,
    name: String,
    override val output: SourceSetOutput
): AbstractKotlinCompilation(target, name) {

}

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