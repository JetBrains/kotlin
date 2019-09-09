/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.EMPTY_CLASSIFIERS_CACHE
import org.jetbrains.kotlin.descriptors.commonizer.core.TestValueParameter.Companion.areEqual
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.mockClassType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.junit.Test

@TypeRefinement
class DefaultValueParameterCommonizerTest : AbstractCommonizerTest<ValueParameter, ValueParameter>() {

    @Test
    fun sameReturnType1() = doTestSuccess(
        mockValueParam("kotlin.String"),
        mockValueParam("kotlin.String"),
        mockValueParam("kotlin.String"),
        mockValueParam("kotlin.String")
    )

    @Test
    fun sameReturnType2() = doTestSuccess(
        mockValueParam("org.sample.Foo"),
        mockValueParam("org.sample.Foo"),
        mockValueParam("org.sample.Foo"),
        mockValueParam("org.sample.Foo")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentReturnTypes1() = doTestFailure(
        mockValueParam("kotlin.String"),
        mockValueParam("kotlin.String"),
        mockValueParam("kotlin.Int")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentReturnTypes2() = doTestFailure(
        mockValueParam("kotlin.String"),
        mockValueParam("kotlin.String"),
        mockValueParam("org.sample.Foo")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentReturnTypes3() = doTestFailure(
        mockValueParam("org.sample.Foo"),
        mockValueParam("org.sample.Foo"),
        mockValueParam("org.sample.Bar")
    )

    @Test
    fun allHaveVararg1() = doTestSuccess(
        mockValueParam("kotlin.String", hasVarargElementType = true),
        mockValueParam("kotlin.String", hasVarargElementType = true),
        mockValueParam("kotlin.String", hasVarargElementType = true),
        mockValueParam("kotlin.String", hasVarargElementType = true)
    )

    @Test
    fun allHaveVararg2() = doTestSuccess(
        mockValueParam("org.sample.Foo", hasVarargElementType = true),
        mockValueParam("org.sample.Foo", hasVarargElementType = true),
        mockValueParam("org.sample.Foo", hasVarargElementType = true),
        mockValueParam("org.sample.Foo", hasVarargElementType = true)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun someDoesNotHaveVararg1() = doTestFailure(
        mockValueParam("kotlin.String", hasVarargElementType = true),
        mockValueParam("kotlin.String", hasVarargElementType = true),
        mockValueParam("kotlin.String", hasVarargElementType = false)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun someDoesNotHaveVararg2() = doTestFailure(
        mockValueParam("org.sample.Foo", hasVarargElementType = false),
        mockValueParam("org.sample.Foo", hasVarargElementType = false),
        mockValueParam("org.sample.Foo", hasVarargElementType = true)
    )

    @Test
    fun allHaveCrossinline() = doTestSuccess(
        mockValueParam("kotlin.String", isCrossinline = true),
        mockValueParam("kotlin.String", isCrossinline = true),
        mockValueParam("kotlin.String", isCrossinline = true),
        mockValueParam("kotlin.String", isCrossinline = true)
    )

    @Test
    fun someHaveCrossinline1() = doTestSuccess(
        mockValueParam("kotlin.String", isCrossinline = false),
        mockValueParam("kotlin.String", isCrossinline = true),
        mockValueParam("kotlin.String", isCrossinline = true),
        mockValueParam("kotlin.String", isCrossinline = false)
    )

    @Test
    fun someHaveCrossinline2() = doTestSuccess(
        mockValueParam("kotlin.String", isCrossinline = false),
        mockValueParam("kotlin.String", isCrossinline = false),
        mockValueParam("kotlin.String", isCrossinline = true),
        mockValueParam("kotlin.String", isCrossinline = true)
    )

    @Test
    fun allHaveNoinline() = doTestSuccess(
        mockValueParam("kotlin.String", isNoinline = true),
        mockValueParam("kotlin.String", isNoinline = true),
        mockValueParam("kotlin.String", isNoinline = true),
        mockValueParam("kotlin.String", isNoinline = true)
    )

    @Test
    fun someHaveNoinline1() = doTestSuccess(
        mockValueParam("kotlin.String", isNoinline = false),
        mockValueParam("kotlin.String", isNoinline = true),
        mockValueParam("kotlin.String", isNoinline = true),
        mockValueParam("kotlin.String", isNoinline = false)
    )

    @Test
    fun someHaveNoinline2() = doTestSuccess(
        mockValueParam("kotlin.String", isNoinline = false),
        mockValueParam("kotlin.String", isNoinline = false),
        mockValueParam("kotlin.String", isNoinline = true),
        mockValueParam("kotlin.String", isNoinline = true)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun anyDeclaresDefaultValue() = doTestFailure(
        mockValueParam("kotlin.String", declaresDefaultValue = false),
        mockValueParam("kotlin.String", declaresDefaultValue = false),
        mockValueParam("kotlin.String", declaresDefaultValue = true)
    )

    override fun createCommonizer() = ValueParameterCommonizer.default(EMPTY_CLASSIFIERS_CACHE)

    override fun isEqual(a: ValueParameter?, b: ValueParameter?) =
        (a === b) || (a != null && b != null && areEqual(EMPTY_CLASSIFIERS_CACHE, a, b))

    internal companion object {
        fun mockValueParam(
            returnTypeFqName: String,
            name: String = "myParameter",
            hasVarargElementType: Boolean = false,
            isCrossinline: Boolean = false,
            isNoinline: Boolean = false,
            declaresDefaultValue: Boolean = false
        ): ValueParameter {
            val returnType = mockClassType(returnTypeFqName).unwrap()

            return TestValueParameter(
                name = Name.identifier(name),
                annotations = Annotations.EMPTY,
                returnType = returnType,
                varargElementType = returnType.takeIf { hasVarargElementType }, // the vararg type itself does not matter here, only it's presence matters
                isCrossinline = isCrossinline,
                isNoinline = isNoinline,
                declaresDefaultValue = declaresDefaultValue
            )
        }
    }
}

internal data class TestValueParameter(
    override val name: Name,
    override val annotations: Annotations,
    override val returnType: UnwrappedType,
    override val varargElementType: UnwrappedType?,
    override val declaresDefaultValue: Boolean,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean
) : ValueParameter {
    companion object {
        fun areEqual(cache: ClassifiersCache, a: ValueParameter, b: ValueParameter): Boolean {
            if (a.name != b.name
                || !areTypesEqual(cache, a.returnType, b.returnType)
                || a.declaresDefaultValue != b.declaresDefaultValue
                || a.isCrossinline != b.isCrossinline
                || a.isNoinline != b.isNoinline
            ) {
                return false
            }

            val aVarargElementType = a.varargElementType
            val bVarargElementType = b.varargElementType

            return (aVarargElementType === bVarargElementType)
                    || (aVarargElementType != null && bVarargElementType != null
                    && areTypesEqual(cache, aVarargElementType, bVarargElementType))
        }
    }
}
