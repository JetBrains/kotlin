/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlin.metadata.ClassKind
import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmEnumEntry
import kotlin.metadata.KmFunction
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty
import kotlin.metadata.KmPropertyAccessorAttributes
import kotlin.metadata.KmType
import kotlin.metadata.KmTypeAlias
import kotlin.metadata.KmTypeParameter
import kotlin.metadata.KmValueParameter
import kotlin.metadata.KmVariance
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.metadata.kind
import kotlin.test.Test
import kotlin.test.assertEquals

class KmEntityProcessingStrategyTest {
    @Test
    fun `type processing test`() {
        val readStrategy = object : KlibModuleFragmentReadStrategy {
            val processedTypes = HashSet<KmType>()

            override fun processType(type: KmType) {
                processedTypes.add(type)
            }
        }

        val writeStrategy = object : KlibModuleFragmentWriteStrategy {
            val processedTypes = HashSet<KmType>()

            override fun processType(type: KmType) {
                processedTypes.add(type)
            }
        }

        val serialized = generateModule().write(writeStrategy)
        KlibModuleMetadata.readStrict(MetadataLibraryProviderImpl(serialized), readStrategy)

        assertEquals(writeStrategy.processedTypes, readStrategy.processedTypes)
        assertEquals(7, readStrategy.processedTypes.size)
    }

    @Test
    fun `annotation processing test`() {
        val readStrategy = object : KlibModuleFragmentReadStrategy {
            val processedAnnotations = HashSet<KmAnnotation>()

            override fun processAnnotation(annotation: KmAnnotation) {
                processedAnnotations += annotation
            }
        }

        val writeStrategy = object : KlibModuleFragmentWriteStrategy {
            val processedAnnotations = HashSet<KmAnnotation>()

            override fun processAnnotation(annotation: KmAnnotation) {
                processedAnnotations += annotation
            }
        }

        val serialized = generateModule().write(writeStrategy)
        KlibModuleMetadata.readStrict(MetadataLibraryProviderImpl(serialized), readStrategy)

        assertEquals(writeStrategy.processedAnnotations, readStrategy.processedAnnotations)
        assertEquals(14, readStrategy.processedAnnotations.size)
    }

    private class MetadataLibraryProviderImpl(
        private val metadata: KlibModuleMetadata.SerializedKlibMetadata,
    ) : KlibModuleMetadata.MetadataLibraryProvider {
        override val moduleHeaderData: ByteArray? get() = metadata.header
        override val packageNames: Set<String> = setOf("")
        override val metadataVersion: KlibMetadataVersion = metadata.metadataVersion
        override fun packageMetadataParts(fqName: String): Set<String> = metadata.fragmentNames.toSet()
        override fun packageMetadata(fqName: String, partName: String): ByteArray = metadata.fragments.single().single()
    }

    private fun generateModule() = KlibModuleMetadata(
        name = "test",
        fragments = listOf(generateFragment()),
        metadataVersion = KlibMetadataVersion.LATEST_STABLE_SUPPORTED,
    )

    private fun generateFragment() = KmModuleFragment().apply {
        fqName = "klib"
        pkg = generatePackage()
        classes.addAll(listOf(generateClass(), generateEnumClass()))
        fileAnnotations.add(KmAnnotation("FileAnnotation", emptyMap()))
    }

    private fun generatePackage() = KmPackage().apply {
        typeAliases.add(KmTypeAlias("TypeAlias").apply {
            underlyingType = generateKmType("sample/One")
            expandedType = generateKmType("sample/Two")
            annotations.add(KmAnnotation("TypeAliasAnnotation", emptyMap()))
        })
    }

    private fun generateClass() = KmClass().apply {
        name = "Class"
        annotations.add(KmAnnotation("ClassAnnotation", emptyMap()))
        constructors.add(KmConstructor().apply {
            annotations.add(KmAnnotation("ConstructorAnnotation", emptyMap()))
        })
        functions.add(KmFunction("function").apply {
            returnType = generateKmType("sample/Three").also { it.annotations.add(KmAnnotation("FunctionReturnTypeAnnotation", emptyMap())) }
            annotations.add(KmAnnotation("FunctionAnnotation", emptyMap()))
            valueParameters.add(KmValueParameter("parameter").apply {
                type = generateKmType("sample/Four")
                annotations.add(KmAnnotation("ParameterAnnotation", emptyMap()))
            })
            typeParameters.add(KmTypeParameter("T", 0, KmVariance.INVARIANT).apply {
                annotations.add(KmAnnotation("FunctionTypeParameterAnnotation", emptyMap()))
            })
            receiverParameterType = generateKmType("sample/Five")
            extensionReceiverParameterAnnotations.add(KmAnnotation("FunctionReceiverAnnotation", emptyMap()))
        })
        properties.add(KmProperty("property").apply {
            returnType = generateKmType("sample/Six")
            annotations.add(KmAnnotation("PropertyAnnotation", emptyMap()))
            getter.annotations.add(KmAnnotation("GetterAnnotation", emptyMap()))
            setter = KmPropertyAccessorAttributes().apply {
                annotations.add(KmAnnotation("SetterAnnotation", emptyMap()))
            }
            receiverParameterType = generateKmType("sample/Seven")
            extensionReceiverParameterAnnotations.add(KmAnnotation("PropertyReceiverAnnotation", emptyMap()))
            backingFieldAnnotations.add(KmAnnotation("BackingFieldAnnotation", emptyMap()))
            delegateFieldAnnotations.add(KmAnnotation("DelegateFieldAnnotation", emptyMap()))
        })
    }

    private fun generateEnumClass() = KmClass().apply {
        name = "EnumClass"
        kind = ClassKind.ENUM_CLASS
        kmEnumEntries.add(KmEnumEntry("entry").apply {
            annotations.add(KmAnnotation("EnumEntryAnnotation", emptyMap()))
        })
    }

    private fun generateKmType(classId: String): KmType = KmType().apply {
        classifier = KmClassifier.Class(classId)
    }
}
