/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.search.GlobalSearchScope
import com.sun.jdi.Location
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.idea.core.KotlinFileTypeFactoryUtils
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil.findFilesWithExactPackage
import org.jetbrains.kotlin.idea.stubindex.StaticFacadeIndexUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

object DebuggerUtils {
    @get:TestOnly
    var forceRanking = false

    fun findSourceFileForClassIncludeLibrarySources(
        project: Project,
        scope: GlobalSearchScope,
        className: JvmClassName,
        fileName: String,
        location: Location? = null
    ): KtFile? {
        return runReadAction {
            findSourceFileForClass(
                project,
                listOf(scope, KotlinSourceFilterScope.librarySources(GlobalSearchScope.allScope(project), project)),
                className,
                fileName,
                location
            )
        }
    }

    fun findSourceFileForClass(
        project: Project,
        scopes: List<GlobalSearchScope>,
        className: JvmClassName,
        fileName: String,
        location: Location?
    ): KtFile? {
        if (!isKotlinSourceFile(fileName)) return null
        if (DumbService.getInstance(project).isDumb) return null

        val partFqName = className.fqNameForClassNameWithoutDollars

        for (scope in scopes) {
            val files = findFilesByNameInPackage(className, fileName, project, scope)

            if (files.isEmpty()) {
                continue
            }

            if (files.size == 1 && !forceRanking || location == null) {
                return files.first()
            }

            StaticFacadeIndexUtil.findFilesForFilePart(partFqName, scope, project)
                .singleOrNull { it.name == fileName }
                ?.let { return it }

            return FileRankingCalculatorForIde.findMostAppropriateSource(files, location)
        }

        return null
    }

    private fun findFilesByNameInPackage(
        className: JvmClassName,
        fileName: String,
        project: Project,
        searchScope: GlobalSearchScope
    ): List<KtFile> {
        val files = findFilesWithExactPackage(className.packageFqName, searchScope, project).filter { it.name == fileName }
        return files.sortedWith(JavaElementFinder.byClasspathComparator(searchScope))
    }

    fun isKotlinSourceFile(fileName: String): Boolean {
        val extension = FileUtilRt.getExtension(fileName).toLowerCase()
        return extension in KotlinFileTypeFactoryUtils.KOTLIN_EXTENSIONS
    }
}
