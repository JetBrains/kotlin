/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

object StaticFacadeIndexUtil {

    // TODO change as we introduce multi-file facades (this will require a separate index)
    @JvmStatic
    fun findFilesForFilePart(
        partFqName: FqName,
        searchScope: GlobalSearchScope,
        project: Project
    ): Collection<KtFile> = runReadAction {
        PackagePartClassUtils.getFilesWithCallables(
            KotlinFileFacadeFqNameIndex.INSTANCE.get(partFqName.asString(), project, searchScope)
        )
    }

    @JvmStatic
    fun getMultifileClassForPart(
        partFqName: FqName,
        searchScope: GlobalSearchScope,
        project: Project
    ): Collection<KtFile> = runReadAction {
        KotlinMultifileClassPartIndex.INSTANCE.get(partFqName.asString(), project, searchScope)
    }
}