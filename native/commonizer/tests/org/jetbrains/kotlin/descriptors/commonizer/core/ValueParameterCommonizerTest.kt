/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.core.CirValueParameterTestImpl.Companion.areEqual
import org.jetbrains.kotlin.descriptors.commonizer.core.TypeCommonizerTest.Companion.areEqual
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.descriptors.commonizer.utils.MOCK_CLASSIFIERS
import org.jetbrains.kotlin.descriptors.commonizer.utils.mockClassType
import org.junit.Test

class ValueParameterCommonizerTest : AbstractCommonizerTest<CirValueParameter, CirValueParameter>() {

    @Test
    fun sameReturnType1() = doTestSuccess(
        expected = mockValueParam("kotlin/String"),
        mockValueParam("kotlin/String"),
        mockValueParam("kotlin/String"),
        mockValueParam("kotlin/String")
    )

    @Test
    fun sameReturnType2() = doTestSuccess(
        expected = mockValueParam("org/sample/Foo"),
        mockValueParam("org/sample/Foo"),
        mockValueParam("org/sample/Foo"),
        mockValueParam("org/sample/Foo")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentReturnTypes1() = doTestFailure(
        mockValueParam("kotlin/String"),
        mockValueParam("kotlin/String"),
        mockValueParam("kotlin/Int")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentReturnTypes2() = doTestFailure(
        mockValueParam("kotlin/String"),
        mockValueParam("kotlin/String"),
        mockValueParam("org/sample/Foo")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentReturnTypes3() = doTestFailure(
        mockValueParam("org/sample/Foo"),
        mockValueParam("org/sample/Foo"),
        mockValueParam("org/sample/Bar")
    )

    @Test
    fun allHaveVararg1() = doTestSuccess(
        expected = mockValueParam("kotlin/String", hasVarargElementType = true),
        mockValueParam("kotlin/String", hasVarargElementType = true),
        mockValueParam("kotlin/String", hasVarargElementType = true),
        mockValueParam("kotlin/String", hasVarargElementType = true)
    )

    @Test
    fun allHaveVararg2() = doTestSuccess(
        expected = mockValueParam("org/sample/Foo", hasVarargElementType = true),
        mockValueParam("org/sample/Foo", hasVarargElementType = true),
        mockValueParam("org/sample/Foo", hasVarargElementType = true),
        mockValueParam("org/sample/Foo", hasVarargElementType = true)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun someDoesNotHaveVararg1() = doTestFailure(
        mockValueParam("kotlin/String", hasVarargElementType = true),
        mockValueParam("kotlin/String", hasVarargElementType = true),
        mockValueParam("kotlin/String", hasVarargElementType = false)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun someDoesNotHaveVararg2() = doTestFailure(
        mockValueParam("org/sample/Foo", hasVarargElementType = false),
        mockValueParam("org/sample/Foo", hasVarargElementType = false),
        mockValueParam("org/sample/Foo", hasVarargElementType = true)
    )

    @Test
    fun allHaveCrossinline() = doTestSuccess(
        expected = mockValueParam("kotlin/String", isCrossinline = true),
        mockValueParam("kotlin/String", isCrossinline = true),
        mockValueParam("kotlin/String", isCrossinline = true),
        mockValueParam("kotlin/String", isCrossinline = true)
    )

    @Test
    fun someHaveCrossinline1() = doTestSuccess(
        expected = mockValueParam("kotlin/String", isCrossinline = false),
        mockValueParam("kotlin/String", isCrossinline = true),
        mockValueParam("kotlin/String", isCrossinline = true),
        mockValueParam("kotlin/String", isCrossinline = false)
    )

    @Test
    fun someHaveCrossinline2() = doTestSuccess(
        expected = mockValueParam("kotlin/String", isCrossinline = false),
        mockValueParam("kotlin/String", isCrossinline = false),
        mockValueParam("kotlin/String", isCrossinline = true),
        mockValueParam("kotlin/String", isCrossinline = true)
    )

    @Test
    fun allHaveNoinline() = doTestSuccess(
        expected = mockValueParam("kotlin/String", isNoinline = true),
        mockValueParam("kotlin/String", isNoinline = true),
        mockValueParam("kotlin/String", isNoinline = true),
        mockValueParam("kotlin/String", isNoinline = true)
    )

    @Test
    fun someHaveNoinline1() = doTestSuccess(
        expected = mockValueParam("kotlin/String", isNoinline = false),
        mockValueParam("kotlin/String", isNoinline = true),
        mockValueParam("kotlin/String", isNoinline = true),
        mockValueParam("kotlin/String", isNoinline = false)
    )

    @Test
    fun someHaveNoinline2() = doTestSuccess(
        expected = mockValueParam("kotlin/String", isNoinline = false),
        mockValueParam("kotlin/String", isNoinline = false),
        mockValueParam("kotlin/String", isNoinline = true),
        mockValueParam("kotlin/String", isNoinline = true)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun anyDeclaresDefaultValue() = doTestFailure(
        mockValueParam("kotlin/String", declaresDefaultValue = false),
        mockValueParam("kotlin/String", declaresDefaultValue = false),
        mockValueParam("kotlin/String", declaresDefaultValue = true)
    )

    override fun createCommonizer() = ValueParameterCommonizer(MOCK_CLASSIFIERS)

    override fun isEqual(a: CirValueParameter?, b: CirValueParameter?) =
        (a === b) || (a != null && b != null && areEqual(MOCK_CLASSIFIERS, a, b))

    internal companion object {
        fun mockValueParam(
            returnTypeClassId: String,
            name: String = "myParameter",
            hasVarargElementType: Boolean = false,
            isCrossinline: Boolean = false,
            isNoinline: Boolean = false,
            declaresDefaultValue: Boolean = false
        ): CirValueParameter {
            val returnType = mockClassType(returnTypeClassId)

            return CirValueParameterTestImpl(
                name = CirName.create(name),
                annotations = emptyList(),
                returnType = returnType,
                varargElementType = returnType.takeIf { hasVarargElementType }, // the vararg type itself does not matter here, only it's presence matters
                isCrossinline = isCrossinline,
                isNoinline = isNoinline,
                declaresDefaultValue = declaresDefaultValue
            )
        }
    }
}

internal data class CirValueParameterTestImpl(
    override val name: CirName,
    override val annotations: List<CirAnnotation>,
    override val returnType: CirType,
    override val varargElementType: CirType?,
    override val declaresDefaultValue: Boolean,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean
) : CirValueParameter {
    companion object {
        fun areEqual(classifiers: CirKnownClassifiers, a: CirValueParameter, b: CirValueParameter): Boolean {
            if (a.name != b.name
                || !areEqual(classifiers, a.returnType, b.returnType)
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
                    && areEqual(classifiers, aVarargElementType, bVarargElementType))
        }
    }
}
