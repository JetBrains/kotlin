/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.cinterop

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.project.model.*
import org.jetbrains.kotlin.project.model.utils.findRefiningFragments
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
    override val containingModule: GradleKpmCinteropModule,
    override val fragmentName: String,
    override val languageSettings: LanguageSettings?
) : KpmFragment {
    override val declaredRefinesDependencies: MutableSet<GradleKpmCinteropFragment> = mutableSetOf()

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
    val konanTarget: KonanTarget
) : GradleKpmCinteropFragment(containingModule, fragmentName, languageSettings), KpmVariant {
    override val variantAttributes: Map<KotlinAttributeKey, String>
        get() = mapOf(
            KotlinPlatformTypeAttribute to KotlinPlatformTypeAttribute.NATIVE,
            KotlinNativeTargetAttribute to konanTarget.name
        )

    override fun toString(): String = "variant $fragmentName in $containingModule"
}

internal fun GradleKpmCinteropModule.applyFragmentRequirements(
    requestingModule: GradleKpmModule,
    requestingFragment: GradleKpmFragment
) {
    val requestingVariants = requestingFragment.containingVariants
    requestingVariants.firstOrNull { it !is GradleKpmNativeVariantInternal }?.let { incompatibleVariant ->
        error("$this can't be configured for $incompatibleVariant")
    }

    val allRequestingFragments = requestingModule.findRefiningFragments(requestingFragment) + requestingFragment

    fun addFragmentWithRefines(fragment: GradleKpmFragment): GradleKpmCinteropFragment? {
        if (fragment !in allRequestingFragments) return null

        val addedFragment = fragments.firstOrNull { it.fragmentName == fragment.fragmentName } ?: run {
            val new = if (fragment is KpmVariant) {
                fragment as GradleKpmNativeVariantInternal
                GradleKpmCinteropVariant(
                    this,
                    fragment.fragmentName,
                    fragment.languageSettings,
                    fragment.konanTarget
                )
            } else {
                GradleKpmCinteropFragment(
                    this,
                    fragment.fragmentName,
                    fragment.languageSettings
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
}
