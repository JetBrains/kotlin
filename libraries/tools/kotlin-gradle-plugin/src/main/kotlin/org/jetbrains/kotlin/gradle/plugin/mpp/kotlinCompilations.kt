/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

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
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal fun KotlinCompilation.composeName(prefix: String? = null, suffix: String? = null): String {
    val compilationNamePart = compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }
    val targetNamePart = target.disambiguationClassifier

    return lowerCamelCaseName(prefix, targetNamePart, compilationNamePart, suffix)
}

internal val KotlinCompilation.defaultSourceSetName: String
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

    override val kotlinSourceSets: MutableSet<KotlinSourceSet> = mutableSetOf()

    override fun source(sourceSet: KotlinSourceSet) {
        if (kotlinSourceSets.add(sourceSet)) {
            with(target.project) {
                whenEvaluated {
                    sourceSet.getSourceSetHierarchy().forEach { sourceSet ->
                        (target.project.tasks.getByName(compileKotlinTaskName) as AbstractKotlinCompile<*>).source(sourceSet.kotlin)

                        // Use `forced = false` since `api`, `implementation`, and `compileOnly` may be missing in some cases like
                        // old Java & Android projects:
                        addExtendsFromRelation(apiConfigurationName, sourceSet.apiConfigurationName, forced = false)
                        addExtendsFromRelation(implementationConfigurationName, sourceSet.implementationConfigurationName, forced = false)
                        addExtendsFromRelation(compileOnlyConfigurationName, sourceSet.compileOnlyConfigurationName, forced = false)

                        if (this is KotlinCompilationToRunnableFiles) {
                            addExtendsFromRelation(runtimeOnlyConfigurationName, sourceSet.runtimeOnlyConfigurationName, forced = false)
                        }
                    }
                }
            }
        }
    }

    override val compileDependencyConfigurationName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "compileClasspath"
        )

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName(
            "compile",
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            "Kotlin",
            target.targetName
        )

    override val compileAllTaskName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            "classes"
        )

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
        dependencies f@{ ConfigureUtil.configure(configureClosure, this@f) }

    override fun toString(): String = "compilation '$compilationName' ($target)"
}

abstract class AbstractKotlinCompilationToRunnableFiles(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilation(target, name), KotlinCompilationToRunnableFiles {
    override val runtimeDependencyConfigurationName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            "runtimeClasspath"
        )

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
    name: String
) : AbstractKotlinCompilationToRunnableFiles(target, name), KotlinCompilationWithResources {
    lateinit var javaSourceSet: SourceSet

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
): AbstractKotlinCompilationToRunnableFiles(target, name)

class KotlinJsCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilationToRunnableFiles(target, name)

class KotlinCommonCompilation(
    target: KotlinTarget,
    name: String,
    override val output: SourceSetOutput
) : AbstractKotlinCompilation(target, name)