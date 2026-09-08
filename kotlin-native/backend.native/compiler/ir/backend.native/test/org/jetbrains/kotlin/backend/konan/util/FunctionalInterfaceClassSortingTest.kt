/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.IrErrorModuleFragment
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.with

class FunctionalInterfaceClassSortingTest {
    @Test
    fun testSorting() {
        val file = newFile()

        file.newClass("Function9")
        file.newClass("Function8")
        file.newClass("Function7")
        file.newClass("Function6")
        file.newClass("Function10")
        file.newClass("Function5")
        file.newClass("Function0")
        file.newClass("Function4")
        file.newClass("Function1")
        file.newClass("Function3")
        file.newClass("Function2")
        file.newClass("KFunction5")
        file.newClass("KFunction4")
        file.newClass("KFunction3")
        file.newClass("KFunction2")
        file.newClass("KFunction1")
        file.newClass("KFunction0")
        file.newClass("KFunction")
        file.newClass("Foo")

        sortDeclarationsInFunctionInterfaceFile(file)

        assertEquals(
                /* expected = */
                listOf(
                        "Foo",

                        "Function0",
                        "Function1",
                        "Function2",
                        "Function3",
                        "Function4",
                        "Function5",
                        "Function6",
                        "Function7",
                        "Function8",
                        "Function9",
                        "Function10",

                        "KFunction",
                        "KFunction0",
                        "KFunction1",
                        "KFunction2",
                        "KFunction3",
                        "KFunction4",
                        "KFunction5",
                ),
                /* actual = */ file.declarations.map { (it as IrDeclarationWithName).name.asString() }
        )
    }

    private companion object {
        fun newFile(): IrFile = IrFileImpl(
                fileEntry = NaiveSourceBasedFileEntryImpl(name = "test.kt"),
                symbol = IrFileSymbolImpl(null),
                packageFqName = FqName.ROOT,
                module = IrErrorModuleFragment, // we don't care here, let it be just an error fragment
        )

        fun IrFile.newClass(name: String): IrClass = with(IrFactoryImpl) {
            buildClass {
                this.name = Name.identifier(name)
            }.also { this@newClass.declarations += it }
        }
    }
}
