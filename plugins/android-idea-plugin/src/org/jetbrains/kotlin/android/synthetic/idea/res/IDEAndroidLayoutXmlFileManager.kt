/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.synthetic.idea.res

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.idea.AndroidXmlVisitor
import org.jetbrains.kotlin.android.synthetic.res.AndroidLayoutXmlFileManager
import org.jetbrains.kotlin.android.synthetic.res.AndroidModuleInfo
import org.jetbrains.kotlin.psi.KtProperty

public class IDEAndroidLayoutXmlFileManager(val module: Module) : AndroidLayoutXmlFileManager(module.project) {

    override val androidModuleInfo: AndroidModuleInfo? by lazy { module.androidFacet?.toAndroidModuleInfo() }

    override fun propertyToXmlAttributes(property: KtProperty): List<PsiElement> {
        val fqPath = property.getFqName()?.pathSegments() ?: return listOf()
        if (fqPath.size() <= AndroidConst.SYNTHETIC_PACKAGE_PATH_LENGTH) return listOf()

        val layoutPackageName = fqPath[AndroidConst.SYNTHETIC_PACKAGE_PATH_LENGTH].asString()
        val layoutFiles = getLayoutXmlFiles()[layoutPackageName]
        if (layoutFiles == null || layoutFiles.isEmpty()) return listOf()

        val propertyName = property.name

        val attributes = arrayListOf<PsiElement>()
        val visitor = AndroidXmlVisitor { retId, wClass, valueElement ->
            if (retId == propertyName) attributes.add(valueElement)
        }

        layoutFiles.forEach { it.accept(visitor) }
        return attributes
    }

    private val Module.androidFacet: AndroidFacet?
        get() = AndroidFacet.getInstance(this)

    private fun AndroidFacet.toAndroidModuleInfo(): AndroidModuleInfo? {
        val applicationPackage = manifest?.getPackage()?.toString()
        val mainResDirectories = allResourceDirectories.map { it.path }

        return if (applicationPackage != null) {
            AndroidModuleInfo(applicationPackage, mainResDirectories)
        }
        else null
    }

}