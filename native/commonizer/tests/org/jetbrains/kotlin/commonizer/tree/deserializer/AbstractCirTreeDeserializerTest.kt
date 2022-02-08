/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import org.jetbrains.kotlin.commonizer.cir.CirFunction
import org.jetbrains.kotlin.commonizer.cir.CirProperty
import org.jetbrains.kotlin.commonizer.tree.*
import org.jetbrains.kotlin.commonizer.utils.KtInlineSourceCommonizerTestCase

abstract class AbstractCirTreeDeserializerTest : KtInlineSourceCommonizerTestCase() {
    protected fun CirTreeModule.assertSinglePackage(): CirTreePackage {
        return packages.singleOrNull()
            ?: kotlin.test.fail("Expected single package. Found ${packages.map { it.pkg.packageName }}")
    }

    protected fun CirTreeModule.assertSingleProperty(): CirProperty {
        return assertSinglePackage().properties.singleOrNull()
            ?: kotlin.test.fail("Expected single property. Found ${assertSinglePackage().properties.map { it.name }}")
    }

    protected fun CirTreeModule.assertSingleFunction(): CirFunction {
        return assertSinglePackage().functions.singleOrNull()
            ?: kotlin.test.fail("Expected single property. Found ${assertSinglePackage().functions.map { it.name }}")
    }

    protected fun CirTreeModule.assertSingleClass(): CirTreeClass {
        return assertSinglePackage().classes.singleOrNull()
            ?: kotlin.test.fail("Expected single class. Found ${assertSinglePackage().classes.map { it.clazz.name }}")
    }

    protected fun CirTreeModule.assertSingleTypeAlias(): CirTreeTypeAlias {
        return assertSinglePackage().typeAliases.singleOrNull()
            ?: kotlin.test.fail("Expected single type alias. Found ${assertSinglePackage().typeAliases.map { it.typeAlias.name }}")
    }
}
