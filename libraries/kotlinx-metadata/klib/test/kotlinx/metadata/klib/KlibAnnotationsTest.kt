/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty
import kotlin.metadata.KmPropertyAccessorAttributes
import kotlin.metadata.KmType
import kotlin.metadata.KmValueParameter
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.test.Test
import kotlin.test.assertEquals

class KlibAnnotationsTest {
    @Test
    fun testKlibAnnotations() {
        val module1 = KmModuleFragment().apply {
            fqName = "klib"
        }
        val klass1 = KmClass().apply {
            name = "Class"
            annotations.add(KmAnnotation("ClassAnnotation", emptyMap()))
            constructors.add(KmConstructor().apply {
                annotations.add(KmAnnotation("ConstructorAnnotation", emptyMap()))
            })
            functions.add(KmFunction("function").apply {
                returnType = stringKmType()
                annotations.add(KmAnnotation("FunctionAnnotation", emptyMap()))
                valueParameters.add(KmValueParameter("parameter").apply {
                    type = stringKmType()
                    annotations.add(KmAnnotation("ParameterAnnotation", emptyMap()))
                })
            })
            properties.add(KmProperty("property").apply {
                returnType = stringKmType()
                annotations.add(KmAnnotation("PropertyAnnotation", emptyMap()))
                getter.annotations.add(KmAnnotation("GetterAnnotation", emptyMap()))
                setter = KmPropertyAccessorAttributes().apply {
                    annotations.add(KmAnnotation("SetterAnnotation", emptyMap()))
                }
            })
        }
        module1.classes.add(klass1)

        val module2 = readWriteModule(module1)

        val klass2 = module2.classes.single()
        assertEquals(klass1.annotations, klass2.annotations)

        assertEquals(klass1.constructors.single().annotations, klass2.constructors.single().annotations)

        val function1 = klass1.functions.single()
        val function2 = klass2.functions.single()
        assertEquals(function1.annotations, function2.annotations)
        assertEquals(function1.valueParameters.single().annotations, function2.valueParameters.single().annotations)

        val property1 = klass1.properties.single()
        val property2 = klass2.properties.single()
        assertEquals(property1.annotations, property2.annotations)
        assertEquals(property1.getter.annotations, property2.getter.annotations)
        assertEquals(property1.setter!!.annotations, property2.setter!!.annotations)
    }

    private fun stringKmType(): KmType = KmType().apply {
        classifier = KmClassifier.Class("kotlin/String")
    }

    private fun readWriteModule(module: KmModuleFragment): KmModuleFragment {
        val metadata = KlibModuleMetadata("klib", listOf(module), emptyList()).write()
        return KlibModuleMetadata.read(object : KlibModuleMetadata.MetadataLibraryProvider {
            override val moduleHeaderData: ByteArray get() = metadata.header
            override fun packageMetadataParts(fqName: String): Set<String> = metadata.fragmentNames.toSet()
            override fun packageMetadata(fqName: String, partName: String): ByteArray = metadata.fragments.single().single()
        }).fragments.single()
    }
}
