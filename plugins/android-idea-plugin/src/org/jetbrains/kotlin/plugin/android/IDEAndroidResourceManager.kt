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

package org.jetbrains.kotlin.plugin.android

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.lang.resolve.android.AndroidConst
import kotlin.properties.Delegates
import org.jetbrains.kotlin.plugin.android.AndroidXmlVisitor
import org.jetbrains.kotlin.lang.resolve.android.AndroidResourceManager
import org.jetbrains.kotlin.lang.resolve.android.AndroidModuleInfo
import org.jetbrains.kotlin.psi.JetProperty

public class IDEAndroidResourceManager(val module: Module) : AndroidResourceManager(module.getProject()) {

    override val androidModuleInfo: AndroidModuleInfo? by Delegates.lazy { module.androidFacet?.toAndroidModuleInfo() }

    override fun propertyToXmlAttributes(property: JetProperty): List<PsiElement> {
        val fqPath = property.getFqName()?.pathSegments() ?: return listOf()
        if (fqPath.size() <= AndroidConst.SYNTHETIC_PACKAGE_PATH_LENGTH) return listOf()

        val layoutPackageName = fqPath[AndroidConst.SYNTHETIC_PACKAGE_PATH_LENGTH].asString()
        val layoutFiles = getLayoutXmlFiles()[layoutPackageName]
        if (layoutFiles == null || layoutFiles.isEmpty()) return listOf()

        val propertyName = property.getName()

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
        val applicationPackage = getManifest()?.getPackage()?.toString()
        val mainResDirectories = getAllResourceDirectories().map { it.getPath() }

        return if (applicationPackage != null) {
            AndroidModuleInfo(applicationPackage, mainResDirectories)
        }
        else null
    }

}