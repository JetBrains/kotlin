/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.mockClassType
import org.jetbrains.kotlin.descriptors.commonizer.mockValueParameter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.junit.Test

@TypeRefinement
class DefaultValueParameterCommonizerTest : AbstractCommonizerTest<ValueParameterDescriptor, ValueParameter>() {

    @Test
    fun sameReturnType1() = doTestSuccess(
        create("kotlin.String"),
        create("kotlin.String").toMockParam(),
        create("kotlin.String").toMockParam(),
        create("kotlin.String").toMockParam()
    )

    @Test
    fun sameReturnType2() = doTestSuccess(
        create("org.sample.Foo"),
        create("org.sample.Foo").toMockParam(),
        create("org.sample.Foo").toMockParam(),
        create("org.sample.Foo").toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun differentReturnTypes1() = doTestFailure(
        create("kotlin.String").toMockParam(),
        create("kotlin.String").toMockParam(),
        create("kotlin.Int").toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun differentReturnTypes2() = doTestFailure(
        create("kotlin.String").toMockParam(),
        create("kotlin.String").toMockParam(),
        create("org.sample.Foo").toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun differentReturnTypes3() = doTestFailure(
        create("org.sample.Foo").toMockParam(),
        create("org.sample.Foo").toMockParam(),
        create("org.sample.Bar").toMockParam()
    )

    @Test
    fun allHaveVararg1() = doTestSuccess(
        create("kotlin.String", hasVarargElementType = true),
        create("kotlin.String", hasVarargElementType = true).toMockParam(),
        create("kotlin.String", hasVarargElementType = true).toMockParam(),
        create("kotlin.String", hasVarargElementType = true).toMockParam()
    )

    @Test
    fun allHaveVararg2() = doTestSuccess(
        create("org.sample.Foo", hasVarargElementType = true),
        create("org.sample.Foo", hasVarargElementType = true).toMockParam(),
        create("org.sample.Foo", hasVarargElementType = true).toMockParam(),
        create("org.sample.Foo", hasVarargElementType = true).toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun someDoesNotHaveVararg1() = doTestFailure(
        create("kotlin.String", hasVarargElementType = true).toMockParam(),
        create("kotlin.String", hasVarargElementType = true).toMockParam(),
        create("kotlin.String", hasVarargElementType = false).toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun someDoesNotHaveVararg2() = doTestFailure(
        create("org.sample.Foo", hasVarargElementType = false).toMockParam(),
        create("org.sample.Foo", hasVarargElementType = false).toMockParam(),
        create("org.sample.Foo", hasVarargElementType = true).toMockParam()
    )

    @Test
    fun allHaveCrossinline() = doTestSuccess(
        create("kotlin.String", isCrossinline = true),
        create("kotlin.String", isCrossinline = true).toMockParam(),
        create("kotlin.String", isCrossinline = true).toMockParam(),
        create("kotlin.String", isCrossinline = true).toMockParam()
    )

    @Test
    fun someHaveCrossinline1() = doTestSuccess(
        create("kotlin.String", isCrossinline = false),
        create("kotlin.String", isCrossinline = true).toMockParam(),
        create("kotlin.String", isCrossinline = true).toMockParam(),
        create("kotlin.String", isCrossinline = false).toMockParam()
    )

    @Test
    fun someHaveCrossinline2() = doTestSuccess(
        create("kotlin.String", isCrossinline = false),
        create("kotlin.String", isCrossinline = false).toMockParam(),
        create("kotlin.String", isCrossinline = true).toMockParam(),
        create("kotlin.String", isCrossinline = true).toMockParam()
    )

    @Test
    fun allHaveNoinline() = doTestSuccess(
        create("kotlin.String", isNoinline = true),
        create("kotlin.String", isNoinline = true).toMockParam(),
        create("kotlin.String", isNoinline = true).toMockParam(),
        create("kotlin.String", isNoinline = true).toMockParam()
    )

    @Test
    fun someHaveNoinline1() = doTestSuccess(
        create("kotlin.String", isNoinline = false),
        create("kotlin.String", isNoinline = true).toMockParam(),
        create("kotlin.String", isNoinline = true).toMockParam(),
        create("kotlin.String", isNoinline = false).toMockParam()
    )

    @Test
    fun someHaveNoinline2() = doTestSuccess(
        create("kotlin.String", isNoinline = false),
        create("kotlin.String", isNoinline = false).toMockParam(),
        create("kotlin.String", isNoinline = true).toMockParam(),
        create("kotlin.String", isNoinline = true).toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun anyDeclaresDefaultValue() = doTestFailure(
        create("kotlin.String").toMockParam(declaresDefaultValue = false),
        create("kotlin.String").toMockParam(declaresDefaultValue = false),
        create("kotlin.String").toMockParam(declaresDefaultValue = true)
    )

    override fun createCommonizer() = ValueParameterCommonizer.default()

    companion object {
        internal fun create(
            returnTypeFqName: String,
            name: String = "myParameter",
            hasVarargElementType: Boolean = false,
            isCrossinline: Boolean = false,
            isNoinline: Boolean = false
        ): ValueParameter {
            val returnType = mockClassType(returnTypeFqName).unwrap()

            return CommonValueParameter(
                name = Name.identifier(name),
                returnType = returnType,
                varargElementType = returnType.takeIf { hasVarargElementType }, // the vararg type itself does not matter here, only it's presence matters
                isCrossinline = isCrossinline,
                isNoinline = isNoinline
            )
        }

        internal fun ValueParameter.toMockParam(
            index: Int = 0,
            declaresDefaultValue: Boolean = false
        ): ValueParameterDescriptor = mockValueParameter(
            name = name.asString(),
            index = index,
            returnType = returnType,
            varargElementType = varargElementType,
            declaresDefaultValue = declaresDefaultValue,
            isCrossinline = isCrossinline,
            isNoinline = isNoinline
        )
    }
}
