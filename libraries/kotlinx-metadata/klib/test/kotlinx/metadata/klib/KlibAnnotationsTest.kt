/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalAnnotationsInMetadata::class)
class KlibAnnotationsTest {
    @Test
    fun testKlibAnnotations() = testKlibAnnotationsWithMetadataVersion(KlibMetadataVersion.LATEST_STABLE_SUPPORTED)

    @Test
    fun testKlibAnnotationsInExtensions() = testKlibAnnotationsWithMetadataVersion(KlibMetadataVersion(2, 0, 0))

    @Test
    fun testKlibAnnotationsInCommonMetadata() = testKlibAnnotationsWithMetadataVersion(
        KlibMetadataVersion.FIRST_WITH_ANNOTATIONS_IN_COMMON_METADATA
    )

    @Test
    fun testKlibAnnotationsCompatibility() {
        val module = generateModule()

        val extensionsSerializedModule = writeModule(module, KlibMetadataVersion(2, 3, 0))
        val commonMetadataSerializedModule = writeModule(module, KlibMetadataVersion.FIRST_WITH_ANNOTATIONS_IN_COMMON_METADATA)

        val byteArray1 = extensionsSerializedModule.fragments.single().single()
        val byteArray2 = commonMetadataSerializedModule.fragments.single().single()
        assertFalse(byteArray1.contentEquals(byteArray2))

        val extensionsDeserializedModule = readModule(extensionsSerializedModule)
        val commonMetadataDeserializedModule = readModule(commonMetadataSerializedModule)

        assertAnnotationsEqual(module, extensionsDeserializedModule)
        assertAnnotationsEqual(extensionsDeserializedModule, commonMetadataDeserializedModule)
    }

    private fun testKlibAnnotationsWithMetadataVersion(metadataVersion: KlibMetadataVersion) {
        val module = generateModule()

        val serializedModule = writeModule(module, metadataVersion)
        val serializedModule2 = writeModule(module, metadataVersion)

        assertFalse { serializedModule.fragments.single().single() === serializedModule2.fragments.single().single() }
        assertContentEquals(serializedModule.fragments.single().single(), serializedModule2.fragments.single().single())

        val deserializedModule = readModule(serializedModule)
        val deserializedModule2 = readModule(serializedModule2)

        assertAnnotationsEqual(module, deserializedModule)
        assertAnnotationsEqual(deserializedModule, deserializedModule2)
    }

    private fun stringKmType(): KmType = KmType().apply {
        classifier = KmClassifier.Class("kotlin/String")
    }

    private fun writeModule(module: KmModuleFragment, metadataVersion: KlibMetadataVersion): KlibModuleMetadata.SerializedKlibMetadata =
        KlibModuleMetadata("klib", listOf(module), emptyList(), metadataVersion).write()

    private fun readModule(metadata: KlibModuleMetadata.SerializedKlibMetadata): KmModuleFragment =
        KlibModuleMetadata.readLenient(object : KlibModuleMetadata.MetadataLibraryProvider {
            override val moduleHeaderData: ByteArray get() = metadata.header
            override val metadataVersion: KlibMetadataVersion = metadata.metadataVersion
            override fun packageMetadataParts(fqName: String): Set<String> = metadata.fragmentNames.toSet()
            override fun packageMetadata(fqName: String, partName: String): ByteArray = metadata.fragments.single().single()
        }).fragments.single()

    private fun assertAnnotationsEqual(module1: KmModuleFragment, module2: KmModuleFragment) {
        val pkg1 = module1.pkg!!
        val pkg2 = module2.pkg!!
        assertEquals(pkg1.typeAliases.single().annotations, pkg2.typeAliases.single().annotations)

        val klass1 = module1.classes.first()
        val klass2 = module2.classes.first()
        assertEquals(klass1.annotations, klass2.annotations)

        assertEquals(klass1.constructors.single().annotations, klass2.constructors.single().annotations)

        val enumKlass1 = module1.classes.last()
        val enumKlass2 = module2.classes.last()
        val enumEntry1 = enumKlass1.kmEnumEntries.single()
        val enumEntry2 = enumKlass2.kmEnumEntries.single()
        assertEquals(enumEntry1.annotations, enumEntry2.annotations)

        val function1 = klass1.functions.single()
        val function2 = klass2.functions.single()
        assertEquals(function1.annotations, function2.annotations)
        assertEquals(function1.valueParameters.single().annotations, function2.valueParameters.single().annotations)
        assertEquals(function1.returnType.annotations, function2.returnType.annotations)
        assertEquals(function1.typeParameters.single().annotations, function2.typeParameters.single().annotations)
        assertEquals(function1.receiverParameterType!!.annotations, function2.receiverParameterType!!.annotations)

        val property1 = klass1.properties.single()
        val property2 = klass2.properties.single()
        assertEquals(property1.annotations, property2.annotations)
        assertEquals(property1.getter.annotations, property2.getter.annotations)
        assertEquals(property1.setter!!.annotations, property2.setter!!.annotations)
        assertEquals(property1.receiverParameterType!!.annotations, property2.receiverParameterType!!.annotations)
        assertEquals(property1.backingFieldAnnotations, property2.backingFieldAnnotations)
        assertEquals(property1.delegateFieldAnnotations, property2.delegateFieldAnnotations)
    }

    private fun generateModule(): KmModuleFragment = KmModuleFragment().apply {
        fqName = "klib"
        pkg = generatePackage()
        classes.addAll(listOf(generateKlass(), generateEnumKlass()))
    }


    private fun generatePackage() = KmPackage().apply {
        typeAliases.add(KmTypeAlias("TypeAlias").apply {
            underlyingType = stringKmType()
            expandedType = stringKmType()
            annotations.add(KmAnnotation("TypeAliasAnnotation", emptyMap()))
        })
    }

    private fun generateKlass() = KmClass().apply {
        name = "Class"
        annotations.add(KmAnnotation("ClassAnnotation", emptyMap()))
        constructors.add(KmConstructor().apply {
            annotations.add(KmAnnotation("ConstructorAnnotation", emptyMap()))
        })
        functions.add(KmFunction("function").apply {
            returnType = stringKmType().also { it.annotations.add(KmAnnotation("FunctionReturnTypeAnnotation", emptyMap())) }
            annotations.add(KmAnnotation("FunctionAnnotation", emptyMap()))
            valueParameters.add(KmValueParameter("parameter").apply {
                type = stringKmType()
                annotations.add(KmAnnotation("ParameterAnnotation", emptyMap()))
            })
            typeParameters.add(KmTypeParameter("T", 0, KmVariance.INVARIANT).apply {
                annotations.add(KmAnnotation("FunctionTypeParameterAnnotation", emptyMap()))
            })
            receiverParameterType = stringKmType()
            extensionReceiverParameterAnnotations.add(KmAnnotation("FunctionReceiverAnnotation", emptyMap()))
        })
        properties.add(KmProperty("property").apply {
            returnType = stringKmType()
            annotations.add(KmAnnotation("PropertyAnnotation", emptyMap()))
            getter.annotations.add(KmAnnotation("GetterAnnotation", emptyMap()))
            setter = KmPropertyAccessorAttributes().apply {
                annotations.add(KmAnnotation("SetterAnnotation", emptyMap()))
            }
            receiverParameterType = stringKmType()
            extensionReceiverParameterAnnotations.add(KmAnnotation("PropertyReceiverAnnotation", emptyMap()))
            backingFieldAnnotations.add(KmAnnotation("BackingFieldAnnotation", emptyMap()))
            delegateFieldAnnotations.add(KmAnnotation("DelegateFieldAnnotation", emptyMap()))
        })
    }

    private fun generateEnumKlass() = KmClass().apply {
        name = "EnumClass"
        kind = ClassKind.ENUM_CLASS
        kmEnumEntries.add(KmEnumEntry("entry").apply {
            annotations.add(KmAnnotation("EnumEntryAnnotation", emptyMap()))
        })
    }
}
