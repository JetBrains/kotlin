@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalObjCName::class)

import objcInstancetype.*
import kotlin.test.*
import kotlinx.cinterop.*

fun testFoo() {
    val foo = Foo()

    val atReturnType: Foo = foo.atReturnType()
    assertEquals(foo, atReturnType)

    val atBlockReturnType: () -> Foo? = Foo.atBlockReturnType()
    assertNotNull(atBlockReturnType())

    val atFunctionReturnType: CPointer<CFunction<() -> Foo?>>? = foo.atFunctionReturnType()
    assertNull(atFunctionReturnType!!())

    val atPointerType: CPointer<ObjCObjectVar<Foo?>>? = foo.atPointerType()
    assertNotNull(atPointerType)
    assertNull(atPointerType.pointed.value)

    val atComplexType: () -> (() -> CPointer<CPointerVar<ObjCObjectVar<Foo?>>>?)? = foo.atComplexType()
    assertNull(atComplexType!!())
}

fun testBar() {
    val bar: BarProtocol = Baz()
    val barMeta: BarProtocolMeta = Baz

    val protocolMethod: BarProtocol? = bar.protocolMethod()
    assertEquals(bar, protocolMethod)

    val protocolClassMethod: BarProtocol = barMeta.protocolClassMethod()
    assertNotNull(protocolClassMethod)
}

fun testBaz() {
    val baz = Baz()

    val atReturnType: Baz = baz.atReturnType()
    assertEquals(baz, atReturnType)

    val atBlockReturnType: () -> Baz? = Baz.atBlockReturnType()
    assertNotNull(atBlockReturnType())

    val atFunctionReturnType: CPointer<CFunction<() -> Baz?>>? = baz.atFunctionReturnType()
    assertNull(atFunctionReturnType!!())

    val atPointerType: CPointer<ObjCObjectVar<Baz?>>? = baz.atPointerType()
    assertNotNull(atPointerType)
    assertNull(atPointerType.pointed.value)

    val atComplexType: () -> (() -> CPointer<CPointerVar<ObjCObjectVar<Baz?>>>?)? = baz.atComplexType()
    assertNull(atComplexType!!())

    val protocolMethod: Baz? = baz.protocolMethod()
    assertEquals(baz, protocolMethod)

    val protocolClassMethod: Baz = Baz.protocolClassMethod()
    assertNotNull(protocolClassMethod)
}

fun main() {
    testFoo()
    testBar()
    testBaz()
}