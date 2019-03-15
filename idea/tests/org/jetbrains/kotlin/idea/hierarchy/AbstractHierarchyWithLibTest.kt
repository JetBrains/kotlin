/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.hierarchy

import com.intellij.ide.hierarchy.HierarchyBrowserBaseEx
import com.intellij.ide.hierarchy.type.TypeHierarchyTreeStructure
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractHierarchyWithLibTest : AbstractHierarchyTest() {
    protected fun doTest(folderName: String) {
        this.folderName = folderName

        val filesToConfigure = filesToConfigure
        val file = filesToConfigure.first()
        val directive = InTextDirectivesUtils.findLinesWithPrefixesRemoved(
            File("${KotlinTestUtils.getHomeDirectory()}/$folderName/$file").readText(),
            "// BASE_CLASS: "
        ).singleOrNull() ?: error("File should contain BASE_CLASS directive")

        doHierarchyTest(
            Computable {
                val targetClass = findTargetJavaClass(directive.trim())

                TypeHierarchyTreeStructure(
                    project,
                    targetClass,
                    HierarchyBrowserBaseEx.SCOPE_PROJECT
                )
            }, *filesToConfigure
        )
    }

    private fun findTargetJavaClass(targetClass: String): PsiClass {
        return JavaFullClassNameIndex.getInstance().get(targetClass.hashCode(), project, GlobalSearchScope.allScope(project)).find {
            it.qualifiedName == targetClass
        } ?: error("Could not find java class: $targetClass")
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = ProjectDescriptorWithStdlibSources.INSTANCE
}