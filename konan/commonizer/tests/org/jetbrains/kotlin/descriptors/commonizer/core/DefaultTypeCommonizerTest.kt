/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mockClassType
import org.jetbrains.kotlin.descriptors.commonizer.mockTAType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.junit.Test

// TODO: add tests for type parameters
@TypeRefinement
class DefaultTypeCommonizerTest : AbstractCommonizerTest<KotlinType, UnwrappedType>() {

    @Test
    fun classTypesInKotlinPackageWithSameName() = doTestSuccess(
        mockClassType("kotlin.collections.List").unwrap(),
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List")
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInKotlinPackageWithDifferentNames1() = doTestFailure(
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.fictitiousPackageName.List")
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInKotlinPackageWithDifferentNames2() = doTestFailure(
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.Set")
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInKotlinPackageWithDifferentNames3() = doTestFailure(
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List"),
        mockClassType("org.sample.Foo")
    )

    @Test
    fun classTypesInKotlinxPackageWithSameName() = doTestSuccess(
        mockClassType("kotlinx.cinterop.CPointer").unwrap(),
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer")
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInKotlinxPackageWithDifferentNames1() = doTestFailure(
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.fictitiousPackageName.CPointer")
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInKotlinxPackageWithDifferentNames2() = doTestFailure(
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.ObjCObject")
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInKotlinxPackageWithDifferentNames3() = doTestFailure(
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("org.sample.Foo")
    )

    @Test
    fun classTypesInUserPackageWithSameName() = doTestSuccess(
        mockClassType("org.sample.Foo").unwrap(),
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Foo")
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInUserPackageWithDifferentNames1() = doTestFailure(
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Foo"),
        mockClassType("org.fictitiousPackageName.Foo")
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInUserPackageWithDifferentNames2() = doTestFailure(
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Bar")
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInUserPackageWithDifferentNames3() = doTestFailure(
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Foo"),
        mockClassType("kotlin.String")
    )

    @Test
    fun classTypesInKotlinPackageWithSameNullability1() = doTestSuccess(
        mockClassType("kotlin.collections.List").unwrap(),
        mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = false)
    )

    @Test
    fun classTypesInKotlinPackageWithSameNullability2() = doTestSuccess(
        mockClassType("kotlin.collections.List", nullable = true).unwrap(),
        mockClassType("kotlin.collections.List", nullable = true),
        mockClassType("kotlin.collections.List", nullable = true),
        mockClassType("kotlin.collections.List", nullable = true)
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInKotlinPackageWithDifferentNullability1() = doTestFailure(
        mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = true)
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInKotlinPackageWithDifferentNullability2() = doTestFailure(
        mockClassType("kotlin.collections.List", nullable = true),
        mockClassType("kotlin.collections.List", nullable = true),
        mockClassType("kotlin.collections.List", nullable = false)
    )

    @Test
    fun classTypesInUserPackageWithSameNullability1() = doTestSuccess(
        mockClassType("org.sample.Foo").unwrap(),
        mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = false)
    )

    @Test
    fun classTypesInUserPackageWithSameNullability2() = doTestSuccess(
        mockClassType("org.sample.Foo", nullable = true).unwrap(),
        mockClassType("org.sample.Foo", nullable = true),
        mockClassType("org.sample.Foo", nullable = true),
        mockClassType("org.sample.Foo", nullable = true)
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInUserPackageWithDifferentNullability1() = doTestFailure(
        mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = true)
    )

    @Test(expected = IllegalStateException::class)
    fun classTypesInUserPackageWithDifferentNullability2() = doTestFailure(
        mockClassType("org.sample.Foo", nullable = true),
        mockClassType("org.sample.Foo", nullable = true),
        mockClassType("org.sample.Foo", nullable = false)
    )

    @Test
    fun taTypesInKotlinPackageWithSameNameAndClass() = doTestSuccess(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") }.unwrap(),
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInKotlinPackageWithDifferentNames() = doTestFailure(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.FictitiousTypeAlias") { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInKotlinPackageWithDifferentClasses() = doTestFailure(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.FictitiousClass") }
    )

    @Test
    fun taTypesInKotlinxPackageWithSameNameAndClass() = doTestSuccess(
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") }.unwrap(),
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInKotlinxPackageWithDifferentNames() = doTestFailure(
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.FictitiousTypeAlias") { mockClassType("kotlinx.cinterop.CPointer") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInKotlinxPackageWithDifferentClasses() = doTestFailure(
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.FictitiousClass") }
    )

    @Test
    fun multilevelTATypesInKotlinPackageWithSameNameAndRightHandSideClass() = doTestSuccess(
        // that's OK as long as the fully expanded right-hand side is the same class
        mockTAType("kotlin.FictitiousTypeAlias") {
            mockClassType("kotlin.FictitiousClass")
        }.unwrap(),

        mockTAType("kotlin.FictitiousTypeAlias") {
            mockClassType("kotlin.FictitiousClass")
        },

        mockTAType("kotlin.FictitiousTypeAlias") {
            mockTAType("kotlin.FictitiousTypeAliasL2") {
                mockClassType("kotlin.FictitiousClass")
            }
        },

        mockTAType("kotlin.FictitiousTypeAlias") {
            mockTAType("kotlin.FictitiousTypeAliasL2") {
                mockTAType("kotlin.FictitiousTypeAliasL3") {
                    mockClassType("kotlin.FictitiousClass")
                }
            }
        }
    )

    @Test
    fun taTypesInUserPackageWithSameNameAndClass() = doTestSuccess(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") }.unwrap(),
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInUserPackageWithDifferentNames() = doTestFailure(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.BarAlias") { mockClassType("org.sample.Foo") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInUserPackageWithDifferentClasses() = doTestFailure(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Bar") }
    )

    @Test(expected = IllegalStateException::class)
    fun multilevelTATypesInUserPackageWithSameNameAndRightHandSideClass() = doTestFailure(
        mockTAType("org.sample.FooAlias") {
            mockClassType("org.sample.Foo")
        },

        mockTAType("org.sample.FooAlias") {
            mockTAType("org.sample.FooAliasL2") {
                mockClassType("org.sample.Foo")
            }
        }
    )

    @Test
    fun taTypesInKotlinPackageWithSameNullability1() = doTestSuccess(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") }.unwrap(),
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test
    fun taTypesInKotlinPackageWithSameNullability2() = doTestSuccess(
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") }.unwrap(),
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInKotlinPackageWithDifferentNullability1() = doTestFailure(
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInKotlinPackageWithDifferentNullability2() = doTestFailure(
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test
    fun taTypesInKotlinPackageWithDifferentNullability3() = doTestSuccess(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") }.unwrap(),
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = false) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = false) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = true) }
    )

    @Test
    fun taTypesInKotlinPackageWithDifferentNullability4() = doTestSuccess(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") }.unwrap(),
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = true) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = true) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = false) }
    )

    @Test
    fun taTypesInUserPackageWithSameNullability1() = doTestSuccess(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") }.unwrap(),
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") }
    )

    @Test
    fun taTypesInUserPackageWithSameNullability2() = doTestSuccess(
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") }.unwrap(),
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInUserPackageWithDifferentNullability1() = doTestFailure(
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInUserPackageWithDifferentNullability2() = doTestFailure(
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInUserPackageWithDifferentNullability3() = doTestFailure(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = false) },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = false) },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = true) }
    )

    @Test(expected = IllegalStateException::class)
    fun taTypesInUserPackageWithDifferentNullability4() = doTestFailure(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = true) },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = true) },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = false) }
    )

    override fun createCommonizer() = TypeCommonizer.default()
    override fun isEqual(a: UnwrappedType?, b: UnwrappedType?) = (a === b) || (a != null && b != null && areTypesEqual(a, b))
}
