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
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.kotlin.android.model.AndroidModuleInfoProvider
import org.jetbrains.kotlin.android.model.AndroidModuleInfoProvider.SourceProviderMirror
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
        get() = AndroidModuleInfoProvider.getInstance(module)?.let { getAndroidModuleInfo(it) }

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
        val psiManager = PsiManager.getInstance(project)
        val layouts = layoutGroup.layouts.map { psiFile ->
            // Sometimes due to a race of later-invoked runnables, the PsiFile can be invalidated; make sure to refresh if possible,
            val layout = if (psiFile.isValid) psiFile else psiManager.findFile(psiFile.virtualFile)

            val resources = arrayListOf<AndroidResource>()
            layout?.accept(AndroidXmlVisitor { id, widgetType, attribute ->
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

    private fun SourceProviderMirror.toVariant() = AndroidVariant(name, resDirectories.map { it.canonicalPath })

    private fun getAndroidModuleInfo(androidInfoProvider: AndroidModuleInfoProvider): AndroidModule? {
        if (androidInfoProvider.module.androidExtensionsIsExperimental) {
            return getAndroidModuleInfoExperimental(androidInfoProvider)
        }

        val applicationPackage = androidInfoProvider.getApplicationPackage()

        if (applicationPackage != null) {
            val mainVariant = androidInfoProvider.getMainSourceProvider()?.toVariant()
            val variantsForFlavorts = androidInfoProvider.getFlavorSourceProviders().map { it.toVariant() }
            val allVariants = listOfNotNull(mainVariant) + variantsForFlavorts

            if (allVariants.isNotEmpty()) {
                return AndroidModule(applicationPackage, allVariants)
            }
        }
        return null
    }

    private fun getAndroidModuleInfoExperimental(androidFacet: AndroidModuleInfoProvider): AndroidModule? {
        val applicationPackage = androidFacet.getApplicationPackage() ?: return null
        val appResourceDirectories = androidFacet.getApplicationResourceDirectories(true).mapNotNull { it.canonicalPath }

        val resDirectoriesForMainVariant = androidFacet.run {
            val resDirsFromSourceProviders = androidFacet.getAllSourceProviders()
                .filter { it.name != "main" }
                .flatMap { it.resDirectories }
                .map { it.canonicalPath }

            appResourceDirectories - resDirsFromSourceProviders
        }

        val variants = mutableListOf(AndroidVariant("main", resDirectoriesForMainVariant))

        androidFacet.getActiveSourceProviders()
            .filter { it.name != "main" }
            .forEach { sourceProvider -> variants += sourceProvider.toVariant() }

        return AndroidModule(applicationPackage, variants)
    }
}