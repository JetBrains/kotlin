/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.cinterop

import org.gradle.api.*
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.project.model.*
import org.jetbrains.kotlin.project.model.utils.findRefiningFragments
import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment
import java.io.File
import javax.inject.Inject

internal open class GradleKpmCinteropModuleFactory(private val project: Project) : NamedDomainObjectFactory<GradleKpmCinteropModule> {
    override fun create(name: String): GradleKpmCinteropModule =
        project.objects.newInstance(GradleKpmCinteropModule::class.java, project, name)
}

internal open class GradleKpmCinteropModule @Inject constructor(
    val project: Project,
    private val cinteropName: String
) : KpmModule, Named {
    override val moduleIdentifier: KpmModuleIdentifier = KpmCinteropModuleIdentifier(project.path, cinteropName)

    override val fragments: MutableSet<GradleKpmCinteropFragment> = mutableSetOf()

    override val variants: Iterable<GradleKpmCinteropVariant>
        get() = fragments.filterIsInstance<GradleKpmCinteropVariant>()

    //isn't used
    override val plugins: Iterable<KpmCompilerPlugin> = emptyList()

    override fun toString(): String = "$moduleIdentifier (Gradle)"
    override fun getName(): String = cinteropName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GradleKpmCinteropModule) return false

        if (moduleIdentifier != other.moduleIdentifier) return false

        return true
    }

    override fun hashCode(): Int {
        return moduleIdentifier.hashCode()
    }
}

internal open class GradleKpmCinteropFragment(
    final override val containingModule: GradleKpmCinteropModule,
    final override val fragmentName: String,
    final override val languageSettings: LanguageSettings?,
    val task: TaskProvider<out DefaultTask>
) : KpmFragment {
    override val declaredRefinesDependencies: MutableSet<GradleKpmCinteropFragment> = mutableSetOf()
    val outputFiles: Provider<FileCollection> = task.map { it.outputs.files }

    //isn't used
    override val kotlinSourceRoots: Iterable<File> = emptyList()
    override val declaredModuleDependencies: Iterable<KpmModuleDependency> = emptyList()

    override fun toString(): String = "fragment $fragmentName in $containingModule"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GradleKpmCinteropFragment) return false

        if (containingModule != other.containingModule) return false
        if (fragmentName != other.fragmentName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containingModule.hashCode()
        result = 31 * result + fragmentName.hashCode()
        return result
    }
}

internal class GradleKpmCinteropVariant(
    containingModule: GradleKpmCinteropModule,
    fragmentName: String,
    languageSettings: LanguageSettings?,
    task: TaskProvider<out DefaultTask>,
    val konanTarget: KonanTarget
) : GradleKpmCinteropFragment(containingModule, fragmentName, languageSettings, task), KpmVariant {
    override val variantAttributes: Map<KotlinAttributeKey, String>
        get() = mapOf(
            KotlinPlatformTypeAttribute to KotlinPlatformTypeAttribute.NATIVE,
            KotlinNativeTargetAttribute to konanTarget.name
        )

    override fun toString(): String = "variant $fragmentName in $containingModule"
}

internal fun GradleKpmCinteropModule.applyFragmentRequirements(requestingFragment: GradleKpmFragment) {
    val requestingModule = requestingFragment.containingModule
    val requestingVariants = requestingFragment.containingVariants
    requestingVariants.firstOrNull { it !is GradleKpmNativeVariantInternal }?.let { incompatibleVariant ->
        error("$this can't be configured for $incompatibleVariant")
    }

    val commonizerTask = project.locateOrRegisterTask<ModuleCommonizerTask>(
        lowerCamelCaseName("commonize", name)
    ) { task ->
        task.description = "Generates all common '$name' libraries."
        task.libraryName.set(name)
    }

    val allRequestingFragments = requestingModule.findRefiningFragments(requestingFragment) + requestingFragment

    fun addFragmentWithRefines(fragment: GradleKpmFragment): GradleKpmCinteropFragment? {
        if (fragment !in allRequestingFragments) return null

        val addedFragment = fragments.firstOrNull { it.fragmentName == fragment.fragmentName } ?: run {
            val new = if (fragment is KpmVariant) {
                fragment as GradleKpmNativeVariantInternal

                val cinteropTask = project.registerCinteropTask(name, fragment.konanTarget)
                commonizerTask.configure { task ->
                    task.dependsOn(cinteropTask)
                    task.libraries.put(fragment.konanTarget, cinteropTask.flatMap { it.outputFile })
                }

                GradleKpmCinteropVariant(
                    this,
                    fragment.fragmentName,
                    fragment.languageSettings,
                    cinteropTask,
                    fragment.konanTarget
                )
            } else {
                GradleKpmCinteropFragment(
                    this,
                    fragment.fragmentName,
                    fragment.languageSettings,
                    project.registerDumbCommonizerTask(name, fragment.fragmentName, commonizerTask)
                )
            }
            fragments.add(new)
            new
        }

        val allAddedRefinesFragments = fragment.declaredRefinesDependencies.mapNotNull { addFragmentWithRefines(it) }
        addedFragment.declaredRefinesDependencies.addAll(allAddedRefinesFragments)

        return addedFragment
    }

    requestingVariants.forEach { addFragmentWithRefines(it) }

    fragments.forEach { fragment ->
        if (fragment !is GradleKpmCinteropVariant) {
            val commonizedKlib = commonizerTask.map { task ->
                val identityString = fragment.toSharedCommonizerTarget()?.identityString.orEmpty()
                val group = project.group.toString().takeIf { it.isNotEmpty() }?.let { it + "_" }.orEmpty()
                val cinteropKlibName = task.libraries.get().values.first().nameWithoutExtension
                task.outputDir.asFile.get().resolve("$identityString/$group$cinteropKlibName")
            }
            (fragment.task as TaskProvider<DumbCommonizerTask>).configure { it.outputFile.set(commonizedKlib) }
        }
    }

    //setup commonizer tasks at the end of construction CinteropModule structure
    //because we can several times add FragmentRequirements to the CinteropModule
    val sharedTargets = fragments.mapNotNull { it.toSharedCommonizerTarget() }
    commonizerTask.configure { it.targets.set(sharedTargets) }
}

private fun GradleKpmCinteropFragment.toSharedCommonizerTarget(): SharedCommonizerTarget? {
    if (this is GradleKpmCinteropVariant) return null
    return SharedCommonizerTarget(
        containingModule.variantsContainingFragment(this).map { (it as GradleKpmCinteropVariant).konanTarget }
    )
}

private fun Project.registerCinteropTask(
    libraryName: String,
    konanTarget: KonanTarget
): TaskProvider<CinteropTask> = locateOrRegisterTask(
    lowerCamelCaseName("cinterop", libraryName, *konanTarget.visibleName.split("_").toTypedArray())
) { task ->
    task.group = KotlinNativeTargetConfigurator.INTEROP_GROUP
    task.description = "Generates Kotlin/Native interop library '$libraryName' of target '${konanTarget.name}'."
    task.enabled = konanTarget.enabledOnCurrentHost

    task.interopName.set(libraryName)
    task.target.set(konanTarget)
}

private fun Project.registerDumbCommonizerTask(
    libraryName: String,
    fragmentName: String,
    parentTask: TaskProvider<ModuleCommonizerTask>
) = locateOrRegisterTask<DumbCommonizerTask>(
    lowerCamelCaseName("commonize", libraryName, "for", fragmentName)
) { task ->
    task.group = KotlinNativeTargetConfigurator.INTEROP_GROUP
    task.description = "Generates common library '$libraryName' for $fragmentName."
    task.dependsOn(parentTask)
}
