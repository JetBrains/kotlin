import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testPropertyOverloadByType() {
    val base = InterfaceBase()
    base.delegate = base

    val delegate1_InterfaceBase: InterfaceBase? = base.delegate
    assertEquals(base, delegate1_InterfaceBase)
    val delegate2_InterfaceBase: InterfaceBase? = base.delegate()
    assertEquals(base, delegate2_InterfaceBase)

    // value of `delegate()` should be zero initially
    val derived = InterfaceDerived()
    val delegate4_Long: Long = derived.delegate()
    assertEquals(0L, delegate4_Long)

    // value of `delegate()` should be changed when `delegate: InterfaceBase` is changed
    derived.delegate = derived
    val delegate3_InterfaceBase: InterfaceBase? = derived.delegate
    assertEquals(derived, delegate3_InterfaceBase)
    val delegate5_Long: Long = derived.delegate()
    assertNotEquals(0L, delegate5_Long)
}
