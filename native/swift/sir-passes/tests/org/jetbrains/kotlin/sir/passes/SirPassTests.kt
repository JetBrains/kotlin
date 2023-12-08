/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.passes

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildForeignFunction
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.constants.*
import org.jetbrains.kotlin.sir.mock.MockFunction
import org.jetbrains.kotlin.sir.mock.MockKotlinType
import org.jetbrains.kotlin.sir.mock.MockParameter
import org.jetbrains.kotlin.sir.passes.asserts.assertSirFunctionsEquals
import org.jetbrains.kotlin.sir.passes.mocks.MockSirFunction
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.sir.passes.translation.ForeignIntoSwiftFunctionTranslationPass
import kotlin.test.Test
import kotlin.test.assertNotNull

class SirPassTests {
    @Test
    fun `foreign toplevel function without params should be translated`() {
        val module = buildModule {
            name = "demo"
        }
        val mySirElement = buildForeignFunction {
            origin = SirOrigin.ForeignEntity(
                MockFunction(
                    fqName = listOf("foo"),
                    parameters = emptyList(),
                    returnType = MockKotlinType(BOOLEAN),
                )
            )
            visibility = SirVisibility.PUBLIC
        }
        mySirElement.parent = module

        val myPass = ForeignIntoSwiftFunctionTranslationPass()
        val result = myPass.run(mySirElement, Unit) as? SirFunction
        assertNotNull(result, "SirFunction should be produced")
        val exp = MockSirFunction(
            name = "foo",
            parameters = emptyList(),
            returnType = SirNominalType(SirSwiftModule.bool),
            parent = module,
        )
        assertSirFunctionsEquals(actual = result, expected = exp)
    }

    @Test
    fun `foreign toplevel function with all params should be translated`() {
        val module = buildModule {
            name = "demo"
        }
        val mySirElement = buildForeignFunction {
            origin = SirOrigin.ForeignEntity(
                MockFunction(
                    fqName = listOf("foo"),
                    parameters = listOf(
                        MockParameter(
                            name = "arg1",
                            type = MockKotlinType(name = BYTE)
                        ),
                        MockParameter(
                            name = "arg2",
                            type = MockKotlinType(name = SHORT)
                        ),
                        MockParameter(
                            name = "arg3",
                            type = MockKotlinType(name = INT)
                        ),
                        MockParameter(
                            name = "arg4",
                            type = MockKotlinType(name = LONG)
                        ),
                        MockParameter(
                            name = "arg5",
                            type = MockKotlinType(name = DOUBLE)
                        ),
                        MockParameter(
                            name = "arg6",
                            type = MockKotlinType(name = FLOAT)
                        ),
                        MockParameter(
                            name = "arg7",
                            type = MockKotlinType(name = BOOLEAN)
                        )
                    ),
                    returnType = MockKotlinType(name = BYTE),
                )
            )
            visibility = SirVisibility.PUBLIC
        }
        mySirElement.parent = module

        val myPass = ForeignIntoSwiftFunctionTranslationPass()
        val result = myPass.run(mySirElement, Unit) as? SirFunction
        assertNotNull(result, "SirFunction should be produced")
        val exp = MockSirFunction(
            name = "foo",
            parameters = listOf(
                SirParameter(argumentName = "arg1", type = SirNominalType(SirSwiftModule.int8)),
                SirParameter(argumentName = "arg2", type = SirNominalType(SirSwiftModule.int16)),
                SirParameter(argumentName = "arg3", type = SirNominalType(SirSwiftModule.int32)),
                SirParameter(argumentName = "arg4", type = SirNominalType(SirSwiftModule.int64)),

                SirParameter(argumentName = "arg5", type = SirNominalType(SirSwiftModule.double)),
                SirParameter(argumentName = "arg6", type = SirNominalType(SirSwiftModule.float)),

                SirParameter(argumentName = "arg7", type = SirNominalType(SirSwiftModule.bool)),
            ),
            returnType = SirNominalType(SirSwiftModule.int8),
            parent = module
        )
        assertSirFunctionsEquals(actual = result, expected = exp)
    }
}
