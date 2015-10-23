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

import com.android.builder.model.SourceProvider
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.idea.AndroidXmlVisitor
import org.jetbrains.kotlin.android.synthetic.res.AndroidLayoutXmlFileManager
import org.jetbrains.kotlin.android.synthetic.res.AndroidModule
import org.jetbrains.kotlin.android.synthetic.res.AndroidVariant
import org.jetbrains.kotlin.android.synthetic.res.AndroidVariantData
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.android.synthetic.AndroidConst.SYNTHETIC_PACKAGE_PATH_LENGTH

public class IDEAndroidLayoutXmlFileManager(val module: Module) : AndroidLayoutXmlFileManager(module.project) {

    override val androidModule: AndroidModule? by lazy { module.androidFacet?.toAndroidModuleInfo() }

    override fun propertyToXmlAttributes(property: KtProperty): List<PsiElement> {
        val fqPath = property.fqName?.pathSegments() ?: return listOf()
        if (fqPath.size <= SYNTHETIC_PACKAGE_PATH_LENGTH) return listOf()

        fun handle(variantData: AndroidVariantData, defaultVariant: Boolean = false): List<PsiElement>? {
            val layoutNamePosition = SYNTHETIC_PACKAGE_PATH_LENGTH + (if (defaultVariant) 0 else 1)
            val layoutName = fqPath[layoutNamePosition].asString()

            val layoutFiles = variantData[layoutName] ?: return null
            if (layoutFiles.isEmpty()) return null

            val propertyName = property.name

            val attributes = arrayListOf<PsiElement>()
            val visitor = AndroidXmlVisitor { retId, wClass, valueElement ->
                if (retId == propertyName) attributes.add(valueElement)
            }

            layoutFiles.forEach { it.accept(visitor) }
            return attributes
        }

        for (variantData in getLayoutXmlFiles()) {
            if (variantData.variant.isMainVariant && fqPath.size == SYNTHETIC_PACKAGE_PATH_LENGTH + 2) {
                handle(variantData, true)?.let { return it }
            }
            else if (fqPath[SYNTHETIC_PACKAGE_PATH_LENGTH].asString() == variantData.variant.name) {
                handle(variantData)?.let { return it }
            }
        }

        return listOf()
    }

    private val Module.androidFacet: AndroidFacet?
        get() = AndroidFacet.getInstance(this)

    private fun SourceProvider.toVariant() = AndroidVariant(name, resDirectories.map { it.absolutePath })

    private fun AndroidFacet.toAndroidModuleInfo(): AndroidModule? {
        val applicationPackage = manifest?.`package`?.toString()

        return if (applicationPackage != null) {
            val mainVariant = mainSourceProvider.toVariant()
            val flavorVariants = flavorSourceProviders?.map { it.toVariant() } ?: listOf()
            AndroidModule(applicationPackage, listOf(mainVariant) + flavorVariants)
        }
        else null
    }

}