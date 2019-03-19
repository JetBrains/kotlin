 /*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.defaultSourceSetLanguageSettingsChecker
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.util.*
import java.util.concurrent.Callable

internal fun KotlinCompilation<*>.composeName(prefix: String? = null, suffix: String? = null): String {
    val compilationNamePart = compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }
    val targetNamePart = target.disambiguationClassifier

    return lowerCamelCaseName(prefix, targetNamePart, compilationNamePart, suffix)
}

internal val KotlinCompilation<*>.defaultSourceSetName: String
    get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName)

abstract class AbstractKotlinCompilation<T : KotlinCommonOptions>(
    target: KotlinTarget,
    override val compilationName: String
) : KotlinCompilation<T>, HasKotlinDependencies {

    override val kotlinOptions: T
        get() = compileKotlinTask.kotlinOptions

    override fun kotlinOptions(configure: T.() -> Unit) =
        configure(kotlinOptions)

    @Suppress("UNCHECKED_CAST")
    override val compileKotlinTask: KotlinCompile<T>
        get() = (target.project.tasks.getByName(compileKotlinTaskName) as KotlinCompile<T>)

    // Don't declare this property in the constructor to avoid NPE
    // when an overriding property of a subclass is accessed instead.
    override val target: KotlinTarget = target

    private val attributeContainer = HierarchyAttributeContainer(target.attributes)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val kotlinSourceSets: MutableSet<KotlinSourceSet> = mutableSetOf()

    override val allKotlinSourceSets: Set<KotlinSourceSet>
        get() = kotlinSourceSets.flatMapTo(mutableSetOf()) { it.getSourceSetHierarchy() }

    override val defaultSourceSet: KotlinSourceSet
        get() = target.project.kotlinExtension.sourceSets.getByName(defaultSourceSetName)

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) =
        configure(defaultSourceSet)

    override val output: KotlinCompilationOutput by lazy {
        DefaultKotlinCompilationOutput(
            target.project,
            Callable { target.project.buildDir.resolve("processedResources/${target.targetName}/$name") })
    }

    open fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Boolean) {
        fun AbstractKotlinCompile<*>.configureAction() {
            source(sourceSet.kotlin)
            sourceFilesExtensions(sourceSet.customSourceFilesExtensions)
            if (addAsCommonSources) {
                commonSourceSet += sourceSet.kotlin
            }
        }

        // Note! Invocation of withType-all results in preliminary task instantiation.
        // After fix of this issue the following code should be uncommented:
//        if (useLazyTaskConfiguration) {
//            (target.project.tasks.named(compileKotlinTaskName) as TaskProvider<AbstractKotlinCompile<*>>).configure {
//                it.configureAction()
//            }
//        }

        target.project.tasks
            // To configure a task that may have not yet been created at this point, use 'withType-matching-all`:
            .withType(AbstractKotlinCompile::class.java)
            .matching { it.name == compileKotlinTaskName }
            .all { compileKotlinTask ->
                compileKotlinTask.configureAction()
            }
    }

    override fun source(sourceSet: KotlinSourceSet) {
        if (kotlinSourceSets.add(sourceSet)) {
            //TODO possibly issue with forced instantiation
            target.project.whenEvaluated {
                sourceSet.getSourceSetHierarchy().forEach { sourceSet ->
                    val isCommonSource =
                        CompilationSourceSetUtil.sourceSetsInMultipleCompilations(project)?.contains(sourceSet.name) ?: false

                    addSourcesToCompileTask(sourceSet, addAsCommonSources = isCommonSource)

                    // Use `forced = false` since `api`, `implementation`, and `compileOnly` may be missing in some cases like
                    // old Java & Android projects:
                    addExtendsFromRelation(apiConfigurationName, sourceSet.apiConfigurationName, forced = false)
                    addExtendsFromRelation(implementationConfigurationName, sourceSet.implementationConfigurationName, forced = false)
                    addExtendsFromRelation(compileOnlyConfigurationName, sourceSet.compileOnlyConfigurationName, forced = false)

                    if (this@AbstractKotlinCompilation is KotlinCompilationToRunnableFiles<*>) {
                        addExtendsFromRelation(runtimeOnlyConfigurationName, sourceSet.runtimeOnlyConfigurationName, forced = false)
                    }

                    if (sourceSet.name != defaultSourceSetName) {
                        kotlinExtension.sourceSets.findByName(defaultSourceSetName)?.let { defaultSourceSet ->
                            // Temporary solution for checking consistency across source sets participating in a compilation that may
                            // not be interconnected with the dependsOn relation: check the settings as if the default source set of
                            // the compilation depends on the one added to the compilation:
                            defaultSourceSetLanguageSettingsChecker.runAllChecks(
                                defaultSourceSet,
                                sourceSet
                            )
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
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationName, "classes")

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

abstract class AbstractKotlinCompilationToRunnableFiles<T : KotlinCommonOptions>(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilation<T>(target, name), KotlinCompilationToRunnableFiles<T> {
    override val runtimeDependencyConfigurationName: String
        get() = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            "runtimeClasspath"
        )

    override lateinit var runtimeDependencyFiles: FileCollection
}

internal fun KotlinCompilation<*>.disambiguateName(simpleName: String): String {
    return lowerCamelCaseName(
        target.disambiguationClassifier,
        compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
        simpleName
    )
}

private object CompilationSourceSetUtil {
    // Cache the results per project
    private val projectSourceSetsInMultipleCompilationsCache = WeakHashMap<Project, Set<String>>()

    fun sourceSetsInMultipleCompilations(project: Project) =
        projectSourceSetsInMultipleCompilationsCache.computeIfAbsent(project) { _ ->
            check(project.state.executed) { "Should only be computed after the project is evaluated" }

            val compilations = project.multiplatformExtensionOrNull?.targets?.flatMap { it.compilations }
                ?: return@computeIfAbsent null

            val sources = compilations
                .flatMap { compilation -> compilation.allKotlinSourceSets.map { sourceSet -> compilation to sourceSet } }
                .groupingBy { (_, sourceSet) -> sourceSet }
                .eachCount()

            HashSet<String>().apply {
                for (entry in sources) {
                    if (entry.value > 1) {
                        add(entry.key.name)
                    }
                }
            }
        }
}
