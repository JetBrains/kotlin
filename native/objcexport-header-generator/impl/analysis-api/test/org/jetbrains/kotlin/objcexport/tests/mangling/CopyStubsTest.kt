package org.jetbrains.kotlin.objcexport.tests.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.mangling.copy
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CopyStubsTest {
    @Test
    fun `test - property copy has changed propertyAttributes only`() {
        val objCProperty = ObjCProperty(
            name = "foo",
            comment = ObjCComment("comment"),
            origin = ObjCExportStubOrigin.Binary(null, null),
            type = ObjCInstanceType,
            propertyAttributes = listOf("propertyAttrs"),
            setterName = "setterName",
            getterName = "getterName",
            declarationAttributes = listOf("declarationAttrs")
        )
        val copy = objCProperty.copy(name = "foo", propertyAttributes = "get=foo_", declarationAttributes = null)
        assertTrue {
            copy.name == "foo" &&
                    copy.comment?.contentLines == listOf("comment") &&
                    copy.origin == ObjCExportStubOrigin.Binary(null, null) &&
                    copy.type == ObjCInstanceType &&
                    copy.propertyAttributes == listOf("propertyAttrs", "get=foo_") &&
                    copy.setterName == "setterName" &&
                    copy.getterName == "getterName" &&
                    copy.declarationAttributes == listOf("declarationAttrs")
        }
    }

    @Test
    fun `test - method copy has changed attributes and selectors`() {
        val objCMethod = ObjCMethod(
            comment = ObjCComment("comment"),
            origin = ObjCExportStubOrigin.Binary(null, null),
            isInstanceMethod = true,
            returnType = ObjCInstanceType,
            selectors = listOf("foo:"),
            parameters = listOf(ObjCParameter("p0", null, ObjCVoidType, null)),
            attributes = listOf("swift_name")
        )
        val copy = objCMethod.copy(
            selectors = listOf("foo_"),
            parameters = listOf("p0:"),
            swiftNameAttribute = "swift_name(foo_(p0))"
        )
        assertTrue {
            copy.comment?.contentLines == listOf("comment") &&
                    copy.origin == ObjCExportStubOrigin.Binary(null, null) &&
                    copy.isInstanceMethod == true &&
                    copy.returnType == ObjCInstanceType &&
                    copy.selectors == listOf("foo_") &&
                    copy.parameters.first().name == "p0" &&
                    copy.attributes == listOf("swift_name(foo_(p0))")
        }
    }
}