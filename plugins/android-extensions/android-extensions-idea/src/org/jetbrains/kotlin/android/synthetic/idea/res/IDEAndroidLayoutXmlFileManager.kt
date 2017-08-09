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
import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.android.synthetic.AndroidConst.SYNTHETIC_PACKAGE_PATH_LENGTH
import org.jetbrains.kotlin.android.synthetic.idea.AndroidPsiTreeChangePreprocessor
import org.jetbrains.kotlin.android.synthetic.idea.AndroidXmlVisitor
import org.jetbrains.kotlin.android.synthetic.idea.androidExtensionsIsExperimental
import org.jetbrains.kotlin.android.synthetic.res.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class IDEAndroidLayoutXmlFileManager(val module: Module) : AndroidLayoutXmlFileManager(module.project) {
    override val androidModule: AndroidModule?
        get() = AndroidFacet.getInstance(module)?.let { getAndroidModuleInfo(it) }

    @Volatile
    private var _moduleData: CachedValue<AndroidModuleData>? = null

    override fun getModuleData(): AndroidModuleData {
        if (androidModule == null) {
            _moduleData = null
        }
        else {
            if (_moduleData == null) {
                _moduleData = cachedValue(project) {
                    CachedValueProvider.Result.create(
                            super.getModuleData(),
                            getPsiTreeChangePreprocessor(), ProjectRootModificationTracker.getInstance(project))
                }
            }
        }
        return _moduleData?.value ?: AndroidModuleData.EMPTY
    }

    private fun getPsiTreeChangePreprocessor(): PsiTreeChangePreprocessor {
        return project.getExtensions(PsiTreeChangePreprocessor.EP_NAME).firstIsInstance<AndroidPsiTreeChangePreprocessor>()
    }

    override fun doExtractResources(layoutGroup: AndroidLayoutGroupData, module: ModuleDescriptor): AndroidLayoutGroup {
        val layouts = layoutGroup.layouts.map { layout ->
            val resources = arrayListOf<AndroidResource>()
            layout.accept(AndroidXmlVisitor { id, widgetType, attribute ->
                resources += parseAndroidResource(id, widgetType, attribute.valueElement)
            })
            AndroidLayout(resources)
        }

        return AndroidLayoutGroup(layoutGroup.name, layouts)
    }

    override fun propertyToXmlAttributes(propertyDescriptor: PropertyDescriptor): List<PsiElement> {
        val fqPath = propertyDescriptor.fqNameUnsafe.pathSegments()
        if (fqPath.size <= SYNTHETIC_PACKAGE_PATH_LENGTH) return listOf()

        fun handle(variantData: AndroidVariantData, defaultVariant: Boolean = false): List<PsiElement>? {
            val layoutNamePosition = SYNTHETIC_PACKAGE_PATH_LENGTH + (if (defaultVariant) 0 else 1)
            val layoutName = fqPath[layoutNamePosition].asString()

            val layoutFiles = variantData.layouts[layoutName] ?: return null
            if (layoutFiles.isEmpty()) return null

            val propertyName = propertyDescriptor.name.asString()

            val attributes = arrayListOf<PsiElement>()
            val visitor = AndroidXmlVisitor { retId, _, valueElement ->
                if (retId.name == propertyName) attributes.add(valueElement)
            }

            layoutFiles.forEach { it.accept(visitor) }
            return attributes
        }

        for (variantData in getModuleData().variants) {
            if (variantData.variant.isMainVariant && fqPath.size == SYNTHETIC_PACKAGE_PATH_LENGTH + 2) {
                handle(variantData, true)?.let { return it }
            }
            else if (fqPath[SYNTHETIC_PACKAGE_PATH_LENGTH].asString() == variantData.variant.name) {
                handle(variantData)?.let { return it }
            }
        }

        return listOf()
    }

    private fun SourceProvider.toVariant() = AndroidVariant(name, resDirectories.map { it.canonicalPath })

    private fun getAndroidModuleInfo(androidFacet: AndroidFacet): AndroidModule? {
        if (androidFacet.module.androidExtensionsIsExperimental) {
            return getAndroidModuleInfoExperimental(androidFacet)
        }

        val applicationPackage = androidFacet.manifest?.`package`?.toString()

        if (applicationPackage != null) {
            val mainVariant = androidFacet.mainSourceProvider.toVariant()

            val method = try { androidFacet::class.java.getMethod("getFlavorSourceProviders") } catch (e: NoSuchMethodException) { null }
            val variants: List<AndroidVariant>? = if (method != null) {
                val sourceProviders = method.invoke(androidFacet) as List<SourceProvider>?
                sourceProviders?.map { it.toVariant() } ?: listOf()
            }
            else {
                val model = AndroidModuleModel.get(androidFacet.module)
                model?.flavorSourceProviders?.map { it.toVariant() } ?: listOf(androidFacet.mainSourceProvider.toVariant())
            }

            if (variants != null) {
                return AndroidModule(applicationPackage, listOf(mainVariant) + variants)
            }
        }
        return null
    }

    private fun getAndroidModuleInfoExperimental(androidFacet: AndroidFacet): AndroidModule? {
        val applicationPackage = androidFacet.manifest?.`package`?.toString() ?: return null

        val variants = mutableListOf(androidFacet.mainSourceProvider.toVariant())

        AndroidModuleModel.get(androidFacet.module)?.let { androidGradleModel ->
            androidGradleModel.activeSourceProviders.filter { it.name != "main" }.forEach { sourceProvider ->
                variants += sourceProvider.toVariant()
            }
        }

        return AndroidModule(applicationPackage, variants)
    }
}