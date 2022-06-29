/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.cinterop

import org.gradle.api.*
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.project.model.*
import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment
import org.jetbrains.kotlin.project.model.utils.withRefiningFragments
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

    fun getFragmentByName(fragmentName: String): GradleKpmCinteropFragment? =
        fragments.firstOrNull { it.fragmentName == fragmentName }

    val commonizerTask = project.locateOrRegisterTask<ModuleCommonizerTask>(
        lowerCamelCaseName("commonize", cinteropName)
    ) { task ->
        task.description = "Generates all common '$cinteropName' libraries."
        task.libraryName.set(cinteropName)
        task.targets.set(project.provider {
            fragments.mapNotNull { it.toSharedCommonizerTarget() }
        })
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
}

internal class GradleKpmCinteropVariant(
    containingModule: GradleKpmCinteropModule,
    fragmentName: String,
    languageSettings: LanguageSettings?,
    task: TaskProvider<out DefaultTask>,
    val konanTarget: KonanTarget
) : GradleKpmCinteropFragment(containingModule, fragmentName, languageSettings, task), KpmVariant {
    override val variantAttributes = mapOf(
        KotlinPlatformTypeAttribute to KotlinPlatformTypeAttribute.NATIVE,
        KotlinNativeTargetAttribute to konanTarget.name
    )

    override fun toString(): String = "variant $fragmentName in $containingModule"
}

internal fun GradleKpmCinteropModule.applyFragmentRequirements(requestingFragment: GradleKpmFragment) {
    addNewFragments(requestingFragment.withRefiningFragments())
    copyRefineEdgesFrom(requestingFragment.containingModule)
}

private fun GradleKpmCinteropModule.addNewFragments(original: Set<KpmFragment>) {
    val currentNames = fragments.map { it.fragmentName }.toSet()
    val newFragments = original
        .filterNot { currentNames.contains(it.fragmentName) }
        .map { copyFragment(it) }
    fragments.addAll(newFragments)
}

private fun GradleKpmCinteropModule.copyRefineEdgesFrom(module: GradleKpmModule) {
    fragments.forEach { fragment ->
        val originFragment = module.fragments.getByName(fragment.fragmentName)
        val originRefineEdges = originFragment.declaredRefinesDependencies.map { it.fragmentName }
        val localRefineEdges = originRefineEdges.mapNotNull { getFragmentByName(it) }
        fragment.declaredRefinesDependencies.addAll(localRefineEdges)
    }
}

private fun GradleKpmCinteropModule.copyFragment(original: KpmFragment) =
    if (original is KpmVariant) {
        original as GradleKpmNativeVariantInternal
        val cinteropTask = registerCinteropTask(original.konanTarget)
        commonizerTask.configure { task ->
            task.dependsOn(cinteropTask)
            task.libraries.put(original.konanTarget, cinteropTask.flatMap { it.outputFile })
        }

        GradleKpmCinteropVariant(
            this,
            original.fragmentName,
            original.languageSettings,
            cinteropTask,
            original.konanTarget
        )
    } else {
        GradleKpmCinteropFragment(
            this,
            original.fragmentName,
            original.languageSettings,
            registerFragmentCommonizerTask(original.fragmentName)
        )
    }

private fun GradleKpmCinteropFragment.toSharedCommonizerTarget(): SharedCommonizerTarget? {
    if (this is GradleKpmCinteropVariant) return null
    return SharedCommonizerTarget(
        containingModule.variantsContainingFragment(this).map { (it as GradleKpmCinteropVariant).konanTarget }
    )
}

private fun GradleKpmCinteropModule.registerCinteropTask(
    konanTarget: KonanTarget
): TaskProvider<CinteropTask> = project.locateOrRegisterTask(
    lowerCamelCaseName("cinterop", name, *konanTarget.visibleName.split("_").toTypedArray())
) { task ->
    task.group = KotlinNativeTargetConfigurator.INTEROP_GROUP
    task.description = "Generates Kotlin/Native interop library '$name' of target '${konanTarget.name}'."
    task.enabled = konanTarget.enabledOnCurrentHost

    task.interopName.set(name)
    task.target.set(konanTarget)
}

private fun GradleKpmCinteropModule.registerFragmentCommonizerTask(
    fragmentName: String
) = project.locateOrRegisterTask<FragmentCommonizerTask>(
    lowerCamelCaseName("commonize", name, "for", fragmentName)
) { task ->
    task.group = KotlinNativeTargetConfigurator.INTEROP_GROUP
    task.description = "Generates common library '$name' for $fragmentName."
    task.dependsOn(commonizerTask)

    task.commonizedModuleDir.set(commonizerTask.map { it.outputDir.get() })
    task.target.set(project.provider { getFragmentByName(fragmentName)!!.toSharedCommonizerTarget() })
}
