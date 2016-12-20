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

package org.jetbrains.kotlin.idea.hierarchy

import com.intellij.ide.hierarchy.HierarchyBrowserBaseEx
import com.intellij.ide.hierarchy.type.TypeHierarchyTreeStructure
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.test.ModuleKind
import org.jetbrains.kotlin.idea.test.configureAs
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractHierarchyWithLibTest : AbstractHierarchyTest() {
    override fun setUp() {
        super.setUp()
        myModule.configureAs(ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES)
    }

    protected fun doTest(folderName: String) {
        this.folderName = folderName

        val filesToConfigure = filesToConfigure
        val file = filesToConfigure.first()
        val directive = InTextDirectivesUtils.findLinesWithPrefixesRemoved(
                File("${KotlinTestUtils.getHomeDirectory()}/$folderName/$file").readText(),
                "// BASE_CLASS: "
        ).singleOrNull() ?: error("File should contain BASE_CLASS directive")

        val targetClass = findTargetJavaClass(directive.trim())
        doHierarchyTest(
                Computable {
                    TypeHierarchyTreeStructure(
                            project,
                            targetClass,
                            if (folderName.contains("annotation")) HierarchyBrowserBaseEx.SCOPE_PROJECT else HierarchyBrowserBaseEx.SCOPE_ALL)
                }, *filesToConfigure)
    }

    private fun findTargetJavaClass(targetClass: String): PsiClass {
        return JavaFullClassNameIndex.getInstance().get(targetClass.hashCode(), project, GlobalSearchScope.allScope(project)).find {
            it.qualifiedName == targetClass
        } ?: error("Could not find java class: $targetClass")
    }
}