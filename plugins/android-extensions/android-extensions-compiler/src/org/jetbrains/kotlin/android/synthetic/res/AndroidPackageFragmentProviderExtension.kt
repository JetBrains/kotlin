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

package org.jetbrains.kotlin.android.synthetic.res

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.descriptors.AndroidSyntheticPackageData
import org.jetbrains.kotlin.android.synthetic.descriptors.AndroidSyntheticPackageFragmentDescriptor
import org.jetbrains.kotlin.android.synthetic.descriptors.LazySyntheticElementResolveContext
import org.jetbrains.kotlin.android.synthetic.descriptors.PredefinedPackageFragmentDescriptor
import org.jetbrains.kotlin.android.synthetic.forEachUntilLast
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.storage.StorageManager

abstract class AndroidPackageFragmentProviderExtension : PackageFragmentProviderExtension {
    protected abstract fun getLayoutXmlFileManager(project: Project, moduleInfo: ModuleInfo?): AndroidLayoutXmlFileManager?

    protected abstract fun isExperimental(moduleInfo: ModuleInfo?): Boolean

    override fun getPackageFragmentProvider(
            project: Project,
            module: ModuleDescriptor,
            storageManager: StorageManager,
            trace: BindingTrace,
            moduleInfo: ModuleInfo?,
            lookupTracker: LookupTracker
    ): PackageFragmentProvider? {
        val isExperimental = isExperimental(moduleInfo)

        val layoutXmlFileManager = getLayoutXmlFileManager(project, moduleInfo) ?: return null

        val moduleData = layoutXmlFileManager.getModuleData()

        val lazyContext = LazySyntheticElementResolveContext(module, storageManager)

        val allPackageDescriptors = arrayListOf<PackageFragmentDescriptor>()
        val packagesToLookupInCompletion = arrayListOf<PackageFragmentDescriptor>()

        // Packages with synthetic properties
        for (variantData in moduleData.variants) {
            for ((layoutName, layouts) in variantData.layouts) {
                fun createPackageFragment(fqName: String, forView: Boolean, isDeprecated: Boolean = false) {
                    val resources = layoutXmlFileManager.extractResources(AndroidLayoutGroupData(layoutName, layouts), module)
                    val packageData = AndroidSyntheticPackageData(moduleData, forView, isDeprecated, resources)
                    val packageDescriptor = AndroidSyntheticPackageFragmentDescriptor(
                            module, FqName(fqName), packageData, lazyContext, storageManager, isExperimental,
                            lookupTracker, layoutName
                    )
                    packagesToLookupInCompletion += packageDescriptor
                    allPackageDescriptors += packageDescriptor
                }

                val packageFqName = AndroidConst.SYNTHETIC_PACKAGE + '.' + variantData.variant.name + '.' + layoutName

                createPackageFragment(packageFqName, false)
                createPackageFragment(packageFqName + ".view", true)
            }
        }

        // Empty middle packages
        AndroidConst.SYNTHETIC_SUBPACKAGES.forEachUntilLast { s ->
            allPackageDescriptors += PredefinedPackageFragmentDescriptor(s, module, storageManager)
        }

        for (variantData in moduleData.variants) {
            val fqName = AndroidConst.SYNTHETIC_PACKAGE + '.' + variantData.variant.name
            allPackageDescriptors += PredefinedPackageFragmentDescriptor(fqName, module, storageManager)
        }

        // Package with clearFindViewByIdCache()
        AndroidConst.SYNTHETIC_SUBPACKAGES.last().let { s ->
            val packageDescriptor = PredefinedPackageFragmentDescriptor(s, module, storageManager, packagesToLookupInCompletion) { descriptor ->
                val widgetReceivers = lazyContext().getWidgetReceivers(forView = false, isExperimental = isExperimental) +
                                      lazyContext().getWidgetReceivers(forView = true, isExperimental = isExperimental)

                widgetReceivers
                        .filter { it.mayHaveCache }
                        .map { genClearCacheFunction(descriptor, it.type) }
            }
            packagesToLookupInCompletion += packageDescriptor
            allPackageDescriptors += packageDescriptor
        }

        return AndroidSyntheticPackageFragmentProvider(allPackageDescriptors)
    }
}

class AndroidSyntheticPackageFragmentProvider(val packageFragments: Collection<PackageFragmentDescriptor>) : PackageFragmentProvider {
    override fun getPackageFragments(fqName: FqName) = packageFragments.filter { it.fqName == fqName }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): List<FqName> {
        return packageFragments.asSequence()
                .map { it.fqName }
                .filter { !it.isRoot && it.parent() == fqName }
                .toList()
    }
}
