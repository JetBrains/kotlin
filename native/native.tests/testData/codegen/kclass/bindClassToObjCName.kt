// DISABLE_NATIVE: isAppleTarget=false
// KIND: STANDALONE
// MODULE: base
// FILE: base.kt

package bindClassToObjCName.base

abstract class Base1
class Base2
open class Base3 : Base1()
class Base4 : Base3()

object BaseObject

class BaseHolder {
    class Nested
    inner class Inner

    companion object
}

// MODULE: gen(base)
// FILE: gen.kt

@file:kotlin.native.internal.objc.BindClassToObjCName(bindClassToObjCName.base.Base1::class, "Base1ObjCName")
@file:kotlin.native.internal.objc.BindClassToObjCName(bindClassToObjCName.base.Base3::class, "Base3ObjCName")
@file:kotlin.native.internal.objc.BindClassToObjCName(bindClassToObjCName.gen.Gen1::class, "Gen1ObjCName")
@file:kotlin.native.internal.objc.BindClassToObjCName(bindClassToObjCName.gen.Gen3::class, "Gen3ObjCName")
@file:kotlin.native.internal.objc.BindClassToObjCName(bindClassToObjCName.base.BaseObject::class, "BaseObjectObjCName")
@file:kotlin.native.internal.objc.BindClassToObjCName(bindClassToObjCName.base.BaseHolder.Nested::class, "BaseNestedObjCName")
@file:kotlin.native.internal.objc.BindClassToObjCName(bindClassToObjCName.base.BaseHolder.Inner::class, "BaseInnerObjCName")
@file:kotlin.native.internal.objc.BindClassToObjCName(bindClassToObjCName.base.BaseHolder.Companion::class, "BaseCompanionObjectObjCName")

package bindClassToObjCName.gen

abstract class Gen1
class Gen2
open class Gen3 : Gen1()
class Gen4 : Gen3()

// MODULE: main(base, gen)
// FILE: main.kt

@file:kotlin.native.internal.objc.BindClassToObjCName(bindClassToObjCName.base.Base4::class, "Base4ObjCName")
@file:kotlin.native.internal.objc.BindClassToObjCName(bindClassToObjCName.gen.Gen4::class, "Gen4ObjCName")

import kotlin.native.internal.reflect.objCNameOrNull
import kotlin.test.*

fun box(): String {
    assertEquals("Base1ObjCName", bindClassToObjCName.base.Base1::class.objCNameOrNull)
    assertNull(bindClassToObjCName.base.Base2::class.objCNameOrNull)
    assertEquals("Base3ObjCName", bindClassToObjCName.base.Base3::class.objCNameOrNull)
    assertEquals("Base4ObjCName", bindClassToObjCName.base.Base4::class.objCNameOrNull)
    assertEquals("Gen1ObjCName", bindClassToObjCName.gen.Gen1::class.objCNameOrNull)
    assertNull(bindClassToObjCName.gen.Gen2::class.objCNameOrNull)
    assertEquals("Gen3ObjCName", bindClassToObjCName.gen.Gen3::class.objCNameOrNull)
    assertEquals("Gen4ObjCName", bindClassToObjCName.gen.Gen4::class.objCNameOrNull)
    assertEquals("BaseObjectObjCName", bindClassToObjCName.base.BaseObject::class.objCNameOrNull)
    assertNull(bindClassToObjCName.base.BaseHolder::class.objCNameOrNull)
    assertEquals("BaseNestedObjCName", bindClassToObjCName.base.BaseHolder.Nested::class.objCNameOrNull)
    assertEquals("BaseInnerObjCName", bindClassToObjCName.base.BaseHolder.Inner::class.objCNameOrNull)
    assertEquals("BaseCompanionObjectObjCName", bindClassToObjCName.base.BaseHolder.Companion::class.objCNameOrNull)
    return "OK"
}