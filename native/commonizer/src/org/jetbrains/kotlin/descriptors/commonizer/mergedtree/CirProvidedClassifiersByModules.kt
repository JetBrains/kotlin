/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import gnu.trove.THashSet
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl

internal class CirProvidedClassifiersByModules(modulesProvider: ModulesProvider) : CirProvidedClassifiers {
    private val classifiers: Set<CirEntityId> = loadClassifiers(modulesProvider)

    override fun hasClassifier(classifierId: CirEntityId): Boolean = classifierId in classifiers
}

private fun loadClassifiers(modulesProvider: ModulesProvider): Set<CirEntityId> {
    val result = THashSet<CirEntityId>()

    modulesProvider.loadModuleInfos().forEach { moduleInfo ->
        val metadata = modulesProvider.loadModuleMetadata(moduleInfo.name)

        for (i in metadata.fragmentNames.indices) {
            val packageFqName = metadata.fragmentNames[i]
            val packageFragments = metadata.fragments[i]

            for (j in packageFragments.indices) {
                val packageFragment: ProtoBuf.PackageFragment = parsePackageFragment(packageFragments[j])

                val classes: List<ProtoBuf.Class> = packageFragment.class_List
                val typeAliases: List<ProtoBuf.TypeAlias> = packageFragment.`package`?.typeAliasList.orEmpty()

                if (classes.isEmpty() && typeAliases.isEmpty())
                    break // this and next package fragments do not contain classifiers and can be skipped

                val packageName = CirPackageName.create(packageFqName)
                val nameResolver = NameResolverImpl(packageFragment.strings, packageFragment.qualifiedNames)

                for (clazz in classes) {
                    if (!nameResolver.isLocalClassName(clazz.fqName)) {
                        val classId = CirEntityId.create(nameResolver.getQualifiedClassName(clazz.fqName))
                        check(classId.packageName == packageName)
                        result += classId
                    }
                }

                for (typeAlias in typeAliases) {
                    val typeAliasId = CirEntityId.create(packageName, CirName.create(nameResolver.getString(typeAlias.name)))
                    result += typeAliasId
                }
            }
        }
    }

    return result
}
