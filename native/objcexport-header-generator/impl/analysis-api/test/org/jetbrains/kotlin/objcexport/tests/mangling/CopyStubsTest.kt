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
            propertyAttributes = ["propertyAttrs"],
            setterName = "setterName",
            getterName = "getterName",
            declarationAttributes = ["declarationAttrs"]
        )
        val copy = objCProperty.copy(name = "foo", propertyAttributes = "get=foo_", declarationAttributes = null)
        assertTrue {
            copy.name == "foo" &&
                    copy.comment?.contentLines == ["comment"] &&
                    copy.origin == ObjCExportStubOrigin.Binary(null, null) &&
                    copy.type == ObjCInstanceType &&
                    copy.propertyAttributes == ["propertyAttrs", "get=foo_"] &&
                    copy.setterName == "setterName" &&
                    copy.getterName == "getterName" &&
                    copy.declarationAttributes == ["declarationAttrs"]
        }
    }

    @Test
    fun `test - method copy has changed attributes and selectors`() {
        val objCMethod = ObjCMethod(
            comment = ObjCComment("comment"),
            origin = ObjCExportStubOrigin.Binary(null, null),
            isInstanceMethod = true,
            returnType = ObjCInstanceType,
            selectors = ["foo:"],
            parameters = [ObjCParameter("p0", null, ObjCVoidType, null)],
            attributes = ["swift_name"]
        )
        val copy = objCMethod.copy(
            mangledSelectors = ["foo_"],
            mangledParameters = ["p0:"],
            swiftNameAttribute = "swift_name(foo_(p0))",
            containingStubName = "ContainingStub",
        )
        assertTrue {
            copy.comment?.contentLines == ["comment"] &&
                    copy.origin == ObjCExportStubOrigin.Binary(null, null) &&
                    copy.isInstanceMethod == true &&
                    copy.returnType == ObjCInstanceType &&
                    copy.selectors == ["foo_"] &&
                    copy.parameters.first().name == "p0" &&
                    copy.attributes == ["swift_name(foo_(p0))"]
        }
    }
}
