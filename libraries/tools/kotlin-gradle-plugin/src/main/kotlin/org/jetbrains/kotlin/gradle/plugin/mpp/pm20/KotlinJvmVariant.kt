/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.BasePluginConvention
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.filterModuleName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.project.model.refinesClosure

open class KotlinJvmVariant(containingModule: KotlinGradleModule, fragmentName: String) :
    KotlinGradleVariantWithRuntimeDependencies(containingModule, fragmentName) {

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.jvm
}

class KotlinJvmVariantCompilationData(val variant: KotlinJvmVariant) : KotlinCompilationData<KotlinJvmOptions> {
    override val project: Project get() = variant.containingModule.project

    override val owner: KotlinJvmVariant get() = variant

    override val compilationPurpose: String
        get() = variant.containingModule.name

    override val compilationClassifier: String
        get() = variant.name

    override val kotlinSourceDirectoriesByFragmentName: Map<String, SourceDirectorySet>
        get() = variant.refinesClosure.filterIsInstance<KotlinGradleVariant>().associate { it.disambiguateName("") to it.kotlinSourceRoots }

    override val output: KotlinCompilationOutput
        get() = variant.compilationOutputs

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName("compile", compilationPurpose.takeIf { it != "main" }, "Kotlin", compilationClassifier)

    override val compileAllTaskName: String
        get() = variant.disambiguateName("classes")

    override val compileDependencyFiles: FileCollection
        get() = variant.compileDependencyFiles

    override val languageSettings: LanguageSettingsBuilder
        get() = variant.languageSettings

    override val platformType: KotlinPlatformType
        get() = variant.platformType

    override val friendPaths: Iterable<FileCollection>
        // TODO for now, all output classes of the module are considered friends, even those not on the classpath
        get() {
            // FIXME support compiling against the JARs
            return variant.containingModule.project.pm20Extension.modules.flatMap { it.variants.map { it.compilationOutputs.classesDirs } }
        }

    override val moduleName: String
        get() = // TODO accurate module names that don't rely on all variants having a main counterpart
            variant.containingModule.project.pm20Extension.modules
                .getByName(KotlinGradleModule.MAIN_MODULE_NAME).variants.findByName(variant.name)?.ownModuleName() ?: ownModuleName

    override val ownModuleName: String
        get() = variant.ownModuleName()

    // TODO pull out to the variant
    override val kotlinOptions: KotlinJvmOptions
        get() = KotlinJvmOptionsImpl()
}

internal fun KotlinGradleVariant.ownModuleName(): String {
    val project = containingModule.project
    val baseName = project.convention.findPlugin(BasePluginConvention::class.java)?.archivesBaseName
        ?: project.name
    val suffix = if (containingModule.moduleClassifier == null) "" else "_${containingModule.moduleClassifier}"
    return filterModuleName("$baseName$suffix")
}
