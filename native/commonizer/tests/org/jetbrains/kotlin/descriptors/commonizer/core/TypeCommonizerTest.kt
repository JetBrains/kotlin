/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.LeafTarget
import org.jetbrains.kotlin.descriptors.commonizer.SharedTarget
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirClassFactory
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeAliasFactory
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderStandardKotlinPackages
import org.jetbrains.kotlin.descriptors.commonizer.utils.mockClassType
import org.jetbrains.kotlin.descriptors.commonizer.utils.mockTAType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation
import org.junit.Before
import org.junit.Test

class TypeCommonizerTest : AbstractCommonizerTest<CirType, CirType>() {

    private lateinit var classifiers: CirKnownClassifiers

    @Before
    fun initialize() {
        // reset cache
        classifiers = CirKnownClassifiers(
            commonized = CirCommonizedClassifiers.default(),
            forwardDeclarations = CirForwardDeclarations.default(),
            dependeeLibraries = mapOf(
                FAKE_SHARED_TARGET to object : CirProvidedClassifiers {
                    override fun hasClassifier(classifierId: ClassId): Boolean = classifierId.packageFqName.isUnderStandardKotlinPackages
                }
            )
        )
    }

    @Test
    fun classTypesInKotlinPackageWithSameName() = doTestSuccess(
        expected = mockClassType("kotlin.collections.List"),
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
        expected = mockClassType("kotlinx.cinterop.CPointer"),
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
        expected = mockClassType("org.sample.Foo"),
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
        expected = mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = false),
        mockClassType("kotlin.collections.List", nullable = false)
    )

    @Test
    fun classTypesInKotlinPackageWithSameNullability2() = doTestSuccess(
        expected = mockClassType("kotlin.collections.List", nullable = true),
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
        expected = mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = false),
        mockClassType("org.sample.Foo", nullable = false)
    )

    @Test
    fun classTypesInUserPackageWithSameNullability2() = doTestSuccess(
        expected = mockClassType("org.sample.Foo", nullable = true),
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
    // why success: matching FQNs from the standard Kotlin packages
    fun taTypesInKotlinPackageWithSameNameAndClass() = doTestSuccess(
        expected = mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
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

    @Test
    // why success: matching FQNs from the standard Kotlin packages
    fun taTypesInKotlinPackageWithDifferentClasses() = doTestSuccess(
        expected = mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.FictitiousClass") }
    )

    @Test
    // why success: matching FQNs from the standard Kotlin packages
    fun taTypesInKotlinxPackageWithSameNameAndClass() = doTestSuccess(
        expected = mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
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

    @Test
    // why success: matching FQNs from the standard Kotlin packages
    fun taTypesInKotlinxPackageWithDifferentClasses() = doTestSuccess(
        expected = mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.CPointer") },
        mockTAType("kotlinx.cinterop.CArrayPointer") { mockClassType("kotlinx.cinterop.FictitiousClass") }
    )

    @Test
    // why success: matching FQNs from the standard Kotlin packages
    fun multilevelTATypesInKotlinPackageWithSameNameAndRightHandSideClass() = doTestSuccess(
        expected = mockTAType("kotlin.FictitiousTypeAlias") {
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
    // why success: lifting up
    fun taTypesInUserPackageWithSameNameAndClass() = doTestSuccess(
        expected = mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
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

    @Test
    // why success: expect class/actual TAs
    fun taTypesInUserPackageWithDifferentClasses() = doTestSuccess(
        expected = mockClassType("org.sample.FooAlias"),
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Bar") }
    )

    @Test
    // why success: short-circuiting & lifting up
    fun multilevelTATypesInUserPackageWithSameNameAndRightHandSideClass1() = doTestSuccess(
        expected = mockTAType("org.sample.FooAlias") {
            mockClassType("org.sample.Foo")
        },

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
    // why success: short-circuiting & lifting up
    fun multilevelTATypesInUserPackageWithSameNameAndRightHandSideClass2() = doTestSuccess(
        expected = mockTAType("org.sample.FooAlias") {
            mockClassType("org.sample.Foo")
        },

        mockTAType("org.sample.FooAlias") {
            mockTAType("org.sample.FooAliasL2") {
                mockClassType("org.sample.Foo")
            }
        },

        mockTAType("org.sample.FooAlias") {
            mockClassType("org.sample.Foo")
        }
    )

    @Test
    // why success: lifting up (inner and outer TAs)
    fun multilevelTATypesInUserPackageWithSameNameAndRightHandSideClass3() = doTestSuccess(
        expected = mockTAType("org.sample.FooAlias") {
            mockTAType("org.sample.FooAliasL2") {
                mockClassType("org.sample.Foo")
            }
        },

        mockTAType("org.sample.FooAlias") {
            mockTAType("org.sample.FooAliasL2") {
                mockClassType("org.sample.Foo")
            }
        },

        mockTAType("org.sample.FooAlias") {
            mockTAType("org.sample.FooAliasL2") {
                mockClassType("org.sample.Foo")
            }
        }
    )

    @Test
    // why success: lifting up outer TA and expect class for inner TA
    fun multilevelTATypesInUserPackageWithSameNameAndRightHandSideClass4() = doTestSuccess(
        expected = mockTAType("org.sample.FooAlias") {
            mockTAType("org.sample.FooAliasL2") {
                mockClassType("org.sample.Foo")
            }
        },

        mockTAType("org.sample.FooAlias") {
            mockTAType("org.sample.FooAliasL2") {
                mockClassType("org.sample.Bar")
            }
        },

        mockTAType("org.sample.FooAlias") {
            mockTAType("org.sample.FooAliasL2") {
                mockClassType("org.sample.Baz")
            }
        }
    )

    @Test
    // why success: types with the same nullability are treated as equal
    fun taTypesInKotlinPackageWithSameNullability1() = doTestSuccess(
        expected = mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") },
        mockTAType("kotlin.sequences.SequenceBuilder", nullable = false) { mockClassType("kotlin.sequences.SequenceScope") }
    )

    @Test
    // why success: types with the same nullability are treated as equal
    fun taTypesInKotlinPackageWithSameNullability2() = doTestSuccess(
        expected = mockTAType("kotlin.sequences.SequenceBuilder", nullable = true) { mockClassType("kotlin.sequences.SequenceScope") },
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
    // why success: nullability of underlying type does not matter if typealias belongs to one of the standard Kotlin packages
    fun taTypesInKotlinPackageWithDifferentNullability3() = doTestSuccess(
        expected = mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = false) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = false) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = false) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = true) }
    )

    @Test
    // why success: nullability of underlying type does not matter if typealias belongs to one of the standard Kotlin packages
    fun taTypesInKotlinPackageWithDifferentNullability4() = doTestSuccess(
        expected = mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = true) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = true) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = true) },
        mockTAType("kotlin.sequences.SequenceBuilder") { mockClassType("kotlin.sequences.SequenceScope", nullable = false) }
    )

    @Test
    // why success: types with the same nullability are treated as equal
    fun taTypesInUserPackageWithSameNullability1() = doTestSuccess(
        expected = mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") },
        mockTAType("org.sample.FooAlias", nullable = false) { mockClassType("org.sample.Foo") }
    )

    @Test
    // why success: types with the same nullability are treated as equal
    fun taTypesInUserPackageWithSameNullability2() = doTestSuccess(
        expected = mockTAType("org.sample.FooAlias", nullable = true) { mockClassType("org.sample.Foo") },
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

    @Test
    // why success: nullability of underlying type does not matter if expect class/actual TAs created
    fun taTypesInUserPackageWithDifferentNullability3() = doTestSuccess(
        expected = mockClassType("org.sample.FooAlias"),
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = false) },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = true) }
    )

    @Test
    // why success: nullability of underlying type does not matter if expect class/actual TAs created
    fun taTypesInUserPackageWithDifferentNullability4() = doTestSuccess(
        expected = mockClassType("org.sample.FooAlias"),
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = true) },
        mockTAType("org.sample.FooAlias") { mockClassType("org.sample.Foo", nullable = false) }
    )

    private fun prepareCache(variants: Array<out KotlinType>) {
        check(variants.isNotEmpty())

        fun recurse(type: KotlinType, index: Int) {
            @Suppress("MoveVariableDeclarationIntoWhen")
            val descriptor = (type.getAbbreviation() ?: type).constructor.declarationDescriptor
            when (descriptor) {
                is ClassDescriptor -> {
                    val classId = descriptor.classId ?: error("No class ID for ${descriptor::class.java}, $descriptor")
                    val node = classifiers.classNode(classId) {
                        buildClassNode(
                            storageManager = LockBasedStorageManager.NO_LOCKS,
                            size = variants.size,
                            classifiers = classifiers,
                            parentCommonDeclaration = null,
                            classId = classId
                        )
                    }
                    node.targetDeclarations[index] = CirClassFactory.create(descriptor)
                }
                is TypeAliasDescriptor -> {
                    val typeAliasId = descriptor.classId ?: error("No class ID for ${descriptor::class.java}, $descriptor")
                    val node = classifiers.typeAliasNode(typeAliasId) {
                        buildTypeAliasNode(
                            storageManager = LockBasedStorageManager.NO_LOCKS,
                            size = variants.size,
                            classifiers = classifiers,
                            typeAliasId = typeAliasId
                        )
                    }
                    node.targetDeclarations[index] = CirTypeAliasFactory.create(descriptor)

                    recurse(descriptor.underlyingType, index) // expand underlying types recursively
                }
                else -> error("Unexpected descriptor of KotlinType: $descriptor, $type")
            }
        }

        variants.forEachIndexed { index, type ->
            recurse(type, index)
        }
    }

    fun doTestSuccess(expected: KotlinType, vararg variants: KotlinType) {
        prepareCache(variants)

        doTestSuccess(
            expected = CirTypeFactory.create(expected),
            variants = variants.map(CirTypeFactory::create).toTypedArray()
        )
    }

    fun doTestFailure(vararg variants: KotlinType, shouldFailOnFirstVariant: Boolean = false) {
        prepareCache(variants)

        doTestFailure(
            variants = variants.map(CirTypeFactory::create).toTypedArray(),
            shouldFailOnFirstVariant = shouldFailOnFirstVariant
        )
    }

    override fun createCommonizer() = TypeCommonizer(classifiers)

    override fun isEqual(a: CirType?, b: CirType?) = (a === b) || (a != null && b != null && areEqual(classifiers, a, b))

    companion object {
        fun areEqual(classifiers: CirKnownClassifiers, a: CirType, b: CirType): Boolean =
            TypeCommonizer(classifiers).run { commonizeWith(a) && commonizeWith(b) }

        private val FAKE_SHARED_TARGET = SharedTarget(setOf(LeafTarget("a"), LeafTarget("b")))

        private fun CirKnownClassifiers.classNode(classId: ClassId, computation: () -> CirClassNode) =
            commonized.classNode(classId) ?: computation()

        private fun CirKnownClassifiers.typeAliasNode(typeAliasId: ClassId, computation: () -> CirTypeAliasNode) =
            commonized.typeAliasNode(typeAliasId) ?: computation()
    }
}
