/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackage
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirPackageFactory
import org.jetbrains.kotlin.name.FqName

class PackageCommonizer : AbstractStandardCommonizer<CirPackage, CirPackage>() {
    private lateinit var fqName: FqName

    override fun commonizationResult() = CirPackageFactory.create(fqName = fqName)

    override fun initialize(first: CirPackage) {
        fqName = first.fqName
    }

    override fun doCommonizeWith(next: CirPackage) = true
}
