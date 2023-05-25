import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testPropertyOverloadByType() {
    val base = InterfaceBase()
    val derived = InterfaceDerived()

    // Simple obvious cases
    base.delegate = base
    val delegate1_InterfaceBase: InterfaceBase? = base.delegate
    assertEquals(base, delegate1_InterfaceBase)
    val delegate2_InterfaceBase: InterfaceBase? = base.delegate()
    assertEquals(base, delegate2_InterfaceBase)
    base.setDelegate(derived)
    assertEquals(derived, base.delegate())

    derived.delegate = base
    val delegate3_InterfaceBase: InterfaceBase? = derived.delegate
    assertEquals(base, delegate3_InterfaceBase)

    // Further it becomes complicated
//    val delegate44_InterfaceBase: InterfaceBase? = derived.delegate()  // error: initializer type mismatch: expected objcTests/InterfaceBase, actual kotlin/String?
    val delegate4_String: String? = derived.delegate()
    assertFalse(delegate4_String is String?)  // Weird but true: object of static type `String?` is not `String?` at runtime.

    //   Should next `assertTrue` line be uncommented, it would cause runtime error in next `assertEquals` line:
    //   kotlin.ClassCastException: class InterfaceBase cannot be cast to class kotlin.String
//     assertTrue(delegate4_String is InterfaceBase)
    assertEquals<Any?>(base as InterfaceBase, delegate4_String as String?)

    assertEquals(base as InterfaceBase, delegate4_String as InterfaceBase)  // K1 warning for `delegate as InterfaceBase`: warning: this cast can never succeed
    assertEquals(base, delegate4_String)
//    assertTrue(base == delegate4_String)  // error: operator '==' cannot be applied to 'objcTests/InterfaceBase' and 'it(kotlin/String & objcTests/InterfaceBase)'
    assertTrue(base.equals(delegate4_String))

//    derived.delegate = "a string" // error: assignment type mismatch: actual type is kotlin/String but objcTests/InterfaceBase was expected
//    derived.setDelegate(base)  // error: argument type mismatch: actual type is objcTests/InterfaceBase but kotlin/String? was expected
    derived.setDelegate("a string")
    val delegate5: String? = derived.delegate()
    assertEquals("a string", delegate5)

    // IntegerPropertyProtocol's property `NSInteger delegate` seems to be hidden with override `NSString* delegate` in InterfaceDerived.
    // This can be demonstrated by uncommenting the following lines
//    derived.delegate = 42    // error: assignment type mismatch: actual type is kotlin/Int but objcTests/InterfaceBase was expected
//    derived.setDelegate(42)  // error: actual type is kotlin/Int but kotlin/String? was expected
//    val delegate6: Int = derived.delegate // error: initializer type mismatch: expected kotlin/Int, actual objcTests/InterfaceBase
}
