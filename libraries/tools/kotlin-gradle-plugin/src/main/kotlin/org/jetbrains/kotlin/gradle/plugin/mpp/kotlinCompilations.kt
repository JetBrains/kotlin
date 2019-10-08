 /*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.TaskState
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.KotlinCompilationsModuleGroups
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

    val compileKotlinTaskHolder: TaskProvider<KotlinCompile<T>>
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

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) = defaultSourceSet.configure()

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

    /** If a compilation is aware of its associate compilations' outputs being added to the classpath in a transformed or packaged way,
     * it should point to those friend artifact files via this property.
     * This is a workaround for Android variants that are compiled against
     * JARs of each other, which is not exposed in the API in any other way than in the consumer's classpath. */
    internal open val friendArtifacts: FileCollection
        get() = target.project.files()

    override val moduleName: String
        get() = KotlinCompilationsModuleGroups.getModuleLeaderCompilation(this).takeIf { it != this }?.ownModuleName ?: ownModuleName

    override fun associateWith(other: KotlinCompilation<*>) {
        require(other.target == target) { "Only associations between compilations of a single target are supported" }
        other as AbstractKotlinCompilation<*>

        _associateWith += other

        addAssociateCompilationDependencies(other)
        KotlinCompilationsModuleGroups.unionModules(this, other)
    }

    protected open fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) {
        target.project.dependencies.run {
            val project = target.project

            add(
                compileDependencyConfigurationName,
                project.files(Callable { other.output.classesDirs }, Callable { other.compileDependencyFiles })
            )

            if (this@AbstractKotlinCompilation is KotlinCompilationToRunnableFiles<*>) {
                add(runtimeDependencyConfigurationName, project.files(Callable { other.output.allOutputs }))
                if (other is KotlinCompilationToRunnableFiles<*>) {
                    add(runtimeDependencyConfigurationName, project.files(Callable { other.runtimeDependencyFiles }))
                }
            }
        }
    }

    private val _associateWith: MutableSet<AbstractKotlinCompilation<*>> = mutableSetOf()

    override val associateWith: List<KotlinCompilation<*>>
        get() = Collections.unmodifiableList(_associateWith.toList())
}

 internal val KotlinCompilation<*>.ownModuleName: String
     get() {
         val project = target.project
         val baseName = project.convention.findPlugin(BasePluginConvention::class.java)?.archivesBaseName
             ?: project.name
         val suffix = if (compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) "" else "_$compilationName"
         return filterModuleName("$baseName$suffix")
     }

 internal val KotlinCompilation<*>.associateWithTransitiveClosure: Iterable<KotlinCompilation<*>>
     get() = mutableSetOf<KotlinCompilation<*>>().apply {
         fun visit(other: KotlinCompilation<*>) {
             if (add(other)) {
                 other.associateWith.forEach(::visit)
             }
         }
         associateWith.forEach(::visit)
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

 private typealias CompilationsBySourceSet = Map<KotlinSourceSet, Set<KotlinCompilation<*>>>

internal object CompilationSourceSetUtil {
    private const val EXT_NAME = "kotlin.compilations.bySourceSets"

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreateProperty(
        project: Project,
        initialize: Property<CompilationsBySourceSet>.() -> Unit
    ): Property<CompilationsBySourceSet> {
        val ext = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        if (!ext.has(EXT_NAME)) {
            ext.set(EXT_NAME, project.objects.property(Any::class.java as Class<CompilationsBySourceSet>).also(initialize))
        }
        return ext.get(EXT_NAME) as Property<CompilationsBySourceSet>
    }

    fun compilationsBySourceSets(project: Project): CompilationsBySourceSet {
        val compilationNamesBySourceSetName = getOrCreateProperty(project) {
            var shouldFinalizeValue = false

            set(project.provider {
                val kotlinExtension = project.kotlinExtension
                val targets = when (kotlinExtension) {
                    is KotlinMultiplatformExtension -> kotlinExtension.targets
                    is KotlinSingleTargetExtension -> listOf(kotlinExtension.target)
                    else -> emptyList()
                }

                val compilations = targets.flatMap { it.compilations }

                val result = compilations
                    .flatMap { compilation -> compilation.allKotlinSourceSets.map { sourceSet -> compilation to sourceSet } }
                    .groupBy(
                        { (_, sourceSet) -> sourceSet },
                        valueTransform = { (compilation, _) -> compilation }
                    )
                    .mapValues { (_, compilations) -> compilations.toSet() }

                if (shouldFinalizeValue) {
                    set(result)
                }

                return@provider result
            })

            project.gradle.taskGraph.whenReady { shouldFinalizeValue = true }

            // In case the value is first queried after the task graph has been calculated, finalize the value as soon as a task executes:
            object : TaskExecutionListener {
                override fun beforeExecute(task: Task) = Unit
                override fun afterExecute(task: Task, state: TaskState) {
                    shouldFinalizeValue = true
                }
            }
        }

        return compilationNamesBySourceSetName.get()
    }

    fun sourceSetsInMultipleCompilations(project: Project) =
        compilationsBySourceSets(project).mapNotNullTo(mutableSetOf()) { (sourceSet, compilations) ->
            sourceSet.name.takeIf { compilations.size > 1 }
        }
}

private val invalidModuleNameCharactersRegex = """[\\/\r\n\t]""".toRegex()

private fun filterModuleName(moduleName: String): String =
    moduleName.replace(invalidModuleNameCharactersRegex, "_")