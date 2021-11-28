/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.project.model.refinesClosure

interface KotlinVariantCompilationDataInternal<T : KotlinCommonOptions> : KotlinVariantCompilationData<T> {
    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName("compile", compilationPurpose.takeIf { it != "main" }, "Kotlin", compilationClassifier)

    override val compileAllTaskName: String
        get() = owner.disambiguateName("classes")

    override val kotlinSourceDirectoriesByFragmentName: Map<String, SourceDirectorySet>
        get() = owner.refinesClosure.filterIsInstance<KotlinGradleVariant>().associate { it.disambiguateName("") to it.kotlinSourceRoots }

    override val friendPaths: Iterable<FileCollection>
        // TODO for now, all output classes of the module are considered friends, even those not on the classpath
        get() {
            // FIXME support compiling against the artifact task outputs
            // TODO note for Android: see the friend artifacts code in KotlinAndroidCompilation
            return owner.containingModule.project.pm20Extension.modules.flatMap { it.variants.map { it.compilationOutputs.classesDirs } }
        }

    override val moduleName: String
        get() = // TODO accurate module names that don't rely on all variants having a main counterpart
            owner.containingModule.project.pm20Extension.modules
                .getByName(KotlinGradleModule.MAIN_MODULE_NAME).variants.findByName(owner.name)?.ownModuleName() ?: ownModuleName

    override val ownModuleName: String
        get() = owner.ownModuleName()
}

fun KotlinCompilationData<*>.isMainCompilationData(): Boolean = when (this) {
    is KotlinCompilation<*> -> isMain()
    else -> compilationPurpose == KotlinGradleModule.MAIN_MODULE_NAME
}