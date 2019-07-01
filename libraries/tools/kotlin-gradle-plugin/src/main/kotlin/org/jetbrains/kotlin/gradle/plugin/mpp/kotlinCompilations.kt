 /*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.defaultSourceSetLanguageSettingsChecker
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.locateTask
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

    val compileKotlinTaskHolder: TaskHolder<KotlinCompile<T>>
        get() = target.project.locateTask(compileKotlinTaskName)!!

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

    open fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
        fun AbstractKotlinCompile<*>.configureAction() {
            source(sourceSet.kotlin)
            sourceFilesExtensions(sourceSet.customSourceFilesExtensions)
            commonSourceSet += project.files(Callable {
                if (addAsCommonSources.value) sourceSet.kotlin else emptyList<Any>()
            })
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

    internal fun addExactSourceSetsEagerly(sourceSets: Set<KotlinSourceSet>) {
        with(target.project) {
            //TODO possibly issue with forced instantiation
            sourceSets.forEach { sourceSet ->
                addSourcesToCompileTask(
                    sourceSet,
                    addAsCommonSources = lazy {
                        CompilationSourceSetUtil.sourceSetsInMultipleCompilations(project).contains(sourceSet.name)
                    }
                )

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

    final override fun source(sourceSet: KotlinSourceSet) {
        if (kotlinSourceSets.add(sourceSet)) {
            target.project.whenEvaluated {
                addExactSourceSetsEagerly(sourceSet.getSourceSetHierarchy())
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

internal object CompilationSourceSetUtil {
    // Store only names in the cache to avoid memory leak through indirect references to the project
    private data class TargetCompilationName(val targetName: String, val compilationName: String) {
        fun toCompilation(project: Project): KotlinCompilation<*>? {
            val kotlinExtension = project.kotlinExtension
            val target = when (kotlinExtension) {
                is KotlinMultiplatformExtension -> kotlinExtension.targets.findByName(targetName)
                is KotlinSingleTargetExtension -> kotlinExtension.target.takeIf { it.name == targetName }
                else -> null
            }
            return target?.compilations?.getByName(compilationName)
        }

        companion object {
            fun from(compilation: KotlinCompilation<*>) = TargetCompilationName(compilation.target.name, compilation.name)
        }
    }

    private val compilationsBySourceSetCache = WeakHashMap<Project, Map<String, Set<TargetCompilationName>>>()

    /** Evaluates once per project. Don't access until all source set dependsOn relationships are built and all source sets are added
     * to the relevant compilations. */
    fun compilationsBySourceSets(project: Project): Map<KotlinSourceSet, Set<KotlinCompilation<*>>> {
        val compilationNamesBySourceSetName = compilationsBySourceSetCache.computeIfAbsent(project) { _ ->
            check(project.state.executed) { "Should only be computed after the project is evaluated" }

            val kotlinExtension = project.kotlinExtension
            val targets = when (kotlinExtension) {
                is KotlinMultiplatformExtension -> kotlinExtension.targets
                is KotlinSingleTargetExtension -> listOf(kotlinExtension.target)
                else -> emptyList()
            }

            val compilations = targets.flatMap { it.compilations }

            compilations
                .flatMap { compilation -> compilation.allKotlinSourceSets.map { sourceSet -> compilation to sourceSet } }
                .groupBy(
                    { (_, sourceSet) -> sourceSet.name },
                    valueTransform = { (compilation, _) -> TargetCompilationName.from(compilation) }
                )
                .mapValues { (_, compilations) -> compilations.toSet() }
        }

        return compilationNamesBySourceSetName.entries.associate { (sourceSetName, compilationNames) ->
            project.kotlinExtension.sourceSets.getByName(sourceSetName).to(
                compilationNames.map { checkNotNull(it.toCompilation(project)) }.toSet()
            )
        }
    }

    fun sourceSetsInMultipleCompilations(project: Project) =
        compilationsBySourceSets(project).mapNotNullTo(mutableSetOf()) { (sourceSet, compilations) ->
            sourceSet.name.takeIf { compilations.size > 1 }
        }
}
