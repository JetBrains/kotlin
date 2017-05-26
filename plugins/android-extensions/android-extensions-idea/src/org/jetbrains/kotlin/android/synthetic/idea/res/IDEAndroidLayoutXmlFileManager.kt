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
import com.android.tools.idea.gradle.AndroidGradleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.android.synthetic.AndroidConst.SYNTHETIC_PACKAGE_PATH_LENGTH
import org.jetbrains.kotlin.android.synthetic.idea.AndroidPsiTreeChangePreprocessor
import org.jetbrains.kotlin.android.synthetic.idea.AndroidXmlVisitor
import org.jetbrains.kotlin.android.synthetic.res.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

class IDEAndroidLayoutXmlFileManager(val module: Module) : AndroidLayoutXmlFileManager(module.project) {
    override val androidModule: AndroidModule?
        get() = module.androidFacet?.toAndroidModuleInfo()

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
        return project.getExtensions(PsiTreeChangePreprocessor.EP_NAME).first { it is AndroidPsiTreeChangePreprocessor }
    }

    override fun doExtractResources(files: List<PsiFile>, module: ModuleDescriptor): List<AndroidLayoutGroup> {
        val layoutGroupFiles = files.groupBy { it.name }
        val layoutGroups = mutableListOf<AndroidLayoutGroup>()

        for ((name, layouts) in layoutGroupFiles) {
            layoutGroups += AndroidLayoutGroup(name, layouts.map { layout ->
                val resources = arrayListOf<AndroidResource>()
                layout.accept(AndroidXmlVisitor { id, widgetType, attribute ->
                    resources += parseAndroidResource(id, widgetType, attribute.valueElement)
                })
                AndroidLayout(resources)
            })
        }

        return layoutGroups
    }


    override fun propertyToXmlAttributes(propertyDescriptor: PropertyDescriptor): List<PsiElement> {
        val fqPath = propertyDescriptor.fqNameUnsafe.pathSegments()
        if (fqPath.size <= SYNTHETIC_PACKAGE_PATH_LENGTH) return listOf()

        fun handle(variantData: AndroidVariantData, defaultVariant: Boolean = false): List<PsiElement>? {
            val layoutNamePosition = SYNTHETIC_PACKAGE_PATH_LENGTH + (if (defaultVariant) 0 else 1)
            val layoutName = fqPath[layoutNamePosition].asString()

            val layoutFiles = variantData[layoutName] ?: return null
            if (layoutFiles.isEmpty()) return null

            val propertyName = propertyDescriptor.name.asString()

            val attributes = arrayListOf<PsiElement>()
            val visitor = AndroidXmlVisitor { retId, _, valueElement ->
                if (retId.name == propertyName) attributes.add(valueElement)
            }

            layoutFiles.forEach { it.accept(visitor) }
            return attributes
        }

        for (variantData in getModuleData()) {
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

    private val Module.androidFacet: AndroidFacet?
        get() = AndroidFacet.getInstance(this)

    private fun AndroidFacet.toAndroidModuleInfo(): AndroidModule? {
        val applicationPackage = manifest?.`package`?.toString() ?: return null

        val allResDirectories = getAppResources(true)?.resourceDirs.orEmpty().mapNotNull { it.canonicalPath }

        val resDirectoriesForMainVariant = run {
            val resDirsFromSourceProviders = AndroidGradleModel.get(module)?.allSourceProviders.orEmpty()
                    .filter { it.name != "main" }
                    .flatMap { it.resDirectories }
                    .map { it.canonicalPath }

            allResDirectories - resDirsFromSourceProviders
        }

        val variants = mutableListOf(AndroidVariant("main", resDirectoriesForMainVariant))

        AndroidGradleModel.get(module)?.let { androidGradleModel ->
            androidGradleModel.activeSourceProviders.filter { it.name != "main" }.forEach { sourceProvider ->
                variants += sourceProvider.toVariant()
            }
        }

        return AndroidModule(applicationPackage, variants)
    }
}