/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KlibKDocTest {
    @Test
    fun testKDocIsWrittenAndRead() {
        val fragment = serializeDeserialize(generateModule(withKDoc = true))

        val typeAlias = fragment.pkg!!.typeAliases.single()
        assertEquals("/** TypeAlias */", typeAlias.kdoc)

        val klass = fragment.classes.first()
        assertEquals("/** Class */", klass.kdoc)
        assertEquals("/** constructor */", klass.constructors.single().kdoc)
        assertEquals("/** function */", klass.functions.single().kdoc)

        val property = klass.properties.single()
        assertEquals("/** property */", property.kdoc)
        assertEquals("/** getter */", property.getterKdoc)
        assertEquals("/** setter */", property.setterKdoc)

        val enumKlass = fragment.classes.last()
        assertEquals("/** EnumClass */", enumKlass.kdoc)
        assertEquals("/** entry */", enumKlass.kmEnumEntries.single().kdoc)
    }

    @Test
    fun testNoKDoc() {
        val fragment = serializeDeserialize(generateModule(withKDoc = false))

        assertNull(fragment.pkg!!.typeAliases.single().kdoc)
        val klass = fragment.classes.first()
        assertNull(klass.kdoc)
        assertNull(klass.constructors.single().kdoc)
        assertNull(klass.functions.single().kdoc)
        val property = klass.properties.single()
        assertNull(property.kdoc)
        assertNull(property.getterKdoc)
        assertNull(property.setterKdoc)
        assertNull(fragment.classes.last().kmEnumEntries.single().kdoc)
    }

    private fun serializeDeserialize(module: KmModuleFragment): KmModuleFragment {
        val metadata = KlibModuleMetadata("klib", listOf(module), KlibMetadataVersion.LATEST_STABLE_SUPPORTED).write()
        return KlibModuleMetadata.readStrict(object : KlibModuleMetadata.MetadataLibraryProvider {
            override val moduleHeaderData: ByteArray? get() = metadata.header
            override val packageFqNames: Set<String> = metadata.fragmentNames.toSet()
            override val metadataVersion: KlibMetadataVersion = metadata.metadataVersion
            override fun packageMetadataParts(fqName: String): Set<String> = metadata.fragmentNames.toSet()
            override fun packageMetadata(fqName: String, partName: String): ByteArray = metadata.fragments.single().single()
        }).fragments.single()
    }

    private fun stringKmType(): KmType = KmType().apply {
        classifier = KmClassifier.Class("kotlin/String")
    }

    private fun generateModule(withKDoc: Boolean): KmModuleFragment = KmModuleFragment().apply {
        fun kdoc(text: String): String? = if (withKDoc) "/** $text */" else null

        fqName = "klib"
        pkg = KmPackage().apply {
            typeAliases.add(KmTypeAlias("TypeAlias").apply {
                underlyingType = stringKmType()
                expandedType = stringKmType()
                kdoc = kdoc("TypeAlias")
            })
        }
        classes.add(KmClass().apply {
            name = "Class"
            kdoc = kdoc("Class")
            constructors.add(KmConstructor().apply {
                kdoc = kdoc("constructor")
            })
            functions.add(KmFunction("function").apply {
                returnType = stringKmType()
                kdoc = kdoc("function")
            })
            properties.add(KmProperty("property").apply {
                returnType = stringKmType()
                setter = KmPropertyAccessorAttributes()
                kdoc = kdoc("property")
                getterKdoc = kdoc("getter")
                setterKdoc = kdoc("setter")
            })
        })
        classes.add(KmClass().apply {
            name = "EnumClass"
            kind = ClassKind.ENUM_CLASS
            kdoc = kdoc("EnumClass")
            kmEnumEntries.add(KmEnumEntry("entry").apply {
                kdoc = kdoc("entry")
            })
        })
    }
}
