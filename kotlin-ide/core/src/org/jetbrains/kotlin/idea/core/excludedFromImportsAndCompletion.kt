/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.codeInsight.JavaProjectCodeInsightSettings
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.psi.KtFile


private val exclusions =
    listOf(
        "kotlin.jvm.internal",
        "kotlin.coroutines.experimental.intrinsics",
        "kotlin.coroutines.intrinsics",
        "kotlin.coroutines.experimental.jvm.internal",
        "kotlin.coroutines.jvm.internal",
        "kotlin.reflect.jvm.internal"
    )

private fun shouldBeHiddenAsInternalImplementationDetail(fqName: String, locationFqName: String) =
    exclusions.any { fqName.startsWith(it) } && (locationFqName.isBlank() || !fqName.startsWith(locationFqName))

/**
 * We do not want to show nothing from "kotlin.coroutines.experimental" when release coroutines are available,
 * since in 1.3 this package is obsolete.
 *
 * However, we still want to show this package when release coroutines are not available.
 */
private fun usesOutdatedCoroutinesPackage(fqName: String, inFile: KtFile): Boolean =
    fqName.startsWith("kotlin.coroutines.experimental.") &&
            inFile.languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)

fun DeclarationDescriptor.isExcludedFromAutoImport(project: Project, inFile: KtFile?): Boolean {
    val fqName = importableFqName?.asString() ?: return false
    return JavaProjectCodeInsightSettings.getSettings(project).isExcluded(fqName) ||
            (inFile != null && usesOutdatedCoroutinesPackage(fqName, inFile)) ||
            shouldBeHiddenAsInternalImplementationDetail(fqName, inFile?.packageFqName?.asString() ?: "")
}
