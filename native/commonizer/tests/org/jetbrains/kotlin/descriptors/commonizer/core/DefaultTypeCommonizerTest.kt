/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirRootNode.ClassifiersCacheImpl
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.buildClassNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.buildTypeAliasNode
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroupMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.mockClassType
import org.jetbrains.kotlin.descriptors.commonizer.utils.mockTAType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation
import org.junit.Before
import org.junit.Test

class DefaultTypeCommonizerTest : AbstractCommonizerTest<CirType, CirType>() {

    private lateinit var cache: ClassifiersCacheImpl

    @Before
    fun initialize() {
        cache = ClassifiersCacheImpl() // reset cache
    }

    @Test
    fun classTypesInKotlinPackageWithSameName() = doTestSuccess(
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInKotlinPackageWithDifferentNames1() = doTestFailure(
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.fictitiousPackageName.List")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInKotlinPackageWithDifferentNames2() = doTestFailure(
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.Set")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInKotlinPackageWithDifferentNames3() = doTestFailure(
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List"),
        mockClassType("org.sample.Foo")
    )

    @Test
    fun classTypesInKotlinxPackageWithSameName() = doTestSuccess(
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInKotlinxPackageWithDifferentNames1() = doTestFailure(
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.fictitiousPackageName.CPointer")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInKotlinxPackageWithDifferentNames2() = doTestFailure(
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.ObjCObject")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInKotlinxPackageWithDifferentNames3() = doTestFailure(
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("kotlinx.cinterop.CPointer"),
        mockClassType("org.sample.Foo")
    )

    @Test
    fun classTypesInUserPackageWithSameName() = doTestSuccess(
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Foo")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInUserPackageWithDifferentNames1() = doTestFailure(
        mockClassType("org.sample.Foo"),
        mockClassType("org.fictitiousPackageName.Foo"),
        shouldFailOnFirstVariant = true
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInUserPackageWithDifferentNames2() = doTestFailure(
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Bar"),
        shouldFailOnFirstVariant = true
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInUserPackageWithDifferentNames3() = doTestFailure(
        mockClassType("org.sample.Foo"),
        mockClassType("kotlin.String"),
        shouldFailOnFirstVariant = true
    )

    @Test
    fun classTypesInKotlinPackageWithSameNullability1() = doTestSuccess(
        mockClassType("kotlin.collections.List"),
        mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = false)
    )

    @Test
    fun classTypesInKotlinPackageWithSameNullability2() = doTestSuccess(
        mockClassType("kotlin.collections.List", nullable = true),
        mockClassType("kotlin.collections.List", nullable = true),
        mockClassType("kotlin.collections.List", nullable = true),
        mockClassType("kotlin.collections.List", nullable = true)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInKotlinPackageWithDifferentNullability1() = doTestFailure(
        mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = true)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInKotlinPackageWithDifferentNullability2() = doTestFailure(
        mockClassType("kotlin.collections.List", nullable = true),
        mockClassType("kotlin.collections.List", nullable = true),
        mockClassType("kotlin.collections.List", nullable = false)
    )

    @Test
    fun classTypesInUserPackageWithSameNullability1() = doTestSuccess(
        mockClassType("org.sample.Foo"),
        mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = false)
    )

    @Test
    fun classTypesInUserPackageWithSameNullability2() = doTestSuccess(
        mockClassType("org.sample.Foo", nullable = true),
        mockClassType("org.sample.Foo", nullable = true),
        mockClassType("org.sample.Foo", nullable = true),
        mockClassType("org.sample.Foo", nullable = true)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInUserPackageWithDifferentNullability1() = doTestFailure(
        mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = true)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun classTypesInUserPackageWithDifferentNullability2() = doTestFailure(
        mockClassType("org.sample.Foo", nullable = true),
        mockClassType("org.sample.Foo", nullable = true),
        mockClassType("org.sample.Foo", nullable = false)
    )

    @Test
    fun taTypesInKotlinPackageWithSameNameAndClass() = doTestSuccess(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInKotlinPackageWithDifferentNames() = doTestFailure(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.FictitiousTypeAlias") { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInKotlinPackageWithDifferentClasses() = doTestFailure(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.FictitiousClass") }
    )

    @Test
    fun taTypesInKotlinxPackageWithSameNameAndClass() = doTestSuccess(
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") }
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInKotlinxPackageWithDifferentNames() = doTestFailure(
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.FictitiousTypeAlias") { mockClassType("kotlinx.cinterop.CPointer") }
    )

    @Test(expected = IllegalCommonizerStateException::class)
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
        },

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
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") }
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInUserPackageWithDifferentNames() = doTestFailure(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.BarAlias") { mockClassType("org.sample.Foo") },
        shouldFailOnFirstVariant = true
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInUserPackageWithDifferentClasses() = doTestFailure(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Bar") },
        shouldFailOnFirstVariant = true
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun multilevelTATypesInUserPackageWithSameNameAndRightHandSideClass() = doTestFailure(
        mockTAType("org.sample.FooAlias") {
            mockClassType("org.sample.Foo")
        },

        mockTAType("org.sample.FooAlias") {
            mockTAType("org.sample.FooAliasL2") {
                mockClassType("org.sample.Foo")
            }
        },
        shouldFailOnFirstVariant = true
    )

    @Test
    fun taTypesInKotlinPackageWithSameNullability1() = doTestSuccess(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test
    fun taTypesInKotlinPackageWithSameNullability2() = doTestSuccess(
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInKotlinPackageWithDifferentNullability1() = doTestFailure(
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInKotlinPackageWithDifferentNullability2() = doTestFailure(
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test
    fun taTypesInKotlinPackageWithDifferentNullability3() = doTestSuccess(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = false) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = false) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = true) }
    )

    @Test
    fun taTypesInKotlinPackageWithDifferentNullability4() = doTestSuccess(
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = true) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = true) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = false) }
    )

    @Test
    fun taTypesInUserPackageWithSameNullability1() = doTestSuccess(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") }
    )

    @Test
    fun taTypesInUserPackageWithSameNullability2() = doTestSuccess(
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") }
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInUserPackageWithDifferentNullability1() = doTestFailure(
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") }
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInUserPackageWithDifferentNullability2() = doTestFailure(
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") }
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInUserPackageWithDifferentNullability3() = doTestFailure(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = false) },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = true) },
        shouldFailOnFirstVariant = true
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun taTypesInUserPackageWithDifferentNullability4() = doTestFailure(
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = true) },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = false) },
        shouldFailOnFirstVariant = true
    )

    private fun prepareCache(variants: Array<out KotlinType>) {
        check(variants.isNotEmpty())

        val classesMap =
            CommonizedGroupMap<FqName, ClassDescriptor>(variants.size)
        val typeAliasesMap =
            CommonizedGroupMap<FqName, TypeAliasDescriptor>(variants.size)

        fun recurse(type: KotlinType, index: Int) {
            @Suppress("MoveVariableDeclarationIntoWhen")
            val descriptor = (type.getAbbreviation() ?: type).constructor.declarationDescriptor
            when (descriptor) {
                is ClassDescriptor -> classesMap[descriptor.fqNameSafe][index] = descriptor
                is TypeAliasDescriptor -> {
                    typeAliasesMap[descriptor.fqNameSafe][index] = descriptor
                    recurse(descriptor.underlyingType, index) // expand underlying types recursively
                }
                else -> error("Unexpected descriptor of KotlinType: $descriptor, $type")
            }
        }

        variants.forEachIndexed { index, type ->
            recurse(type, index)
        }

        for ((_, classesGroup) in classesMap) {
            buildClassNode(LockBasedStorageManager.NO_LOCKS, cache, null, classesGroup.toList())
        }

        for ((_, typeAliasesGroup) in typeAliasesMap) {
            buildTypeAliasNode(LockBasedStorageManager.NO_LOCKS, cache, typeAliasesGroup.toList())
        }
    }

    fun doTestSuccess(expected: KotlinType, vararg variants: KotlinType) {
        prepareCache(variants)

        doTestSuccess(
            expected = CirType.create(expected),
            variants = *variants.map(CirType.Companion::create).toTypedArray()
        )
    }

    fun doTestFailure(vararg variants: KotlinType, shouldFailOnFirstVariant: Boolean = false) {
        prepareCache(variants)

        doTestFailure(
            variants = *variants.map(CirType.Companion::create).toTypedArray(),
            shouldFailOnFirstVariant = shouldFailOnFirstVariant
        )
    }

    override fun createCommonizer() = TypeCommonizer.default(cache)

    override fun isEqual(a: CirType?, b: CirType?) = (a === b) || (a != null && b != null && areTypesEqual(cache, a, b))
}
