/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import kotlin.metadata.internal.common.KmModuleFragment
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.fqName
import org.jetbrains.kotlin.commonizer.cir.CirModule
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
import org.jetbrains.kotlin.commonizer.metadata.utils.SerializedMetadataLibraryProvider
import org.jetbrains.kotlin.commonizer.tree.CirTreeModule
import org.jetbrains.kotlin.commonizer.utils.foldToMap
import org.jetbrains.kotlin.library.SerializedMetadata

internal class CirTreeModuleDeserializer(
    private val packageDeserializer: CirTreePackageDeserializer
) {
    operator fun invoke(metadata: SerializedMetadata, typeResolver: CirTypeResolver): CirTreeModule {
        val module = KlibModuleMetadata.read(SerializedMetadataLibraryProvider(metadata))

        val fragmentsByPackage: Map<CirPackageName, Collection<KmModuleFragment>> = module.fragments.foldToMap { fragment ->
            fragment.fqName?.let(CirPackageName.Companion::create)
                ?: error("A fragment without FQ name in module ${module.name}: $fragment")
        }

        val packages = fragmentsByPackage.map { (packageName, fragments) ->
            packageDeserializer(packageName, fragments, typeResolver)
        }

        return CirTreeModule(
            module = CirModule.create(CirName.create(module.name)),
            packages = packages
        )
    }
}
