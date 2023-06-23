import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testPropertyOverloadByType2() {
    val base = InterfaceBase2()
    base.delegate = base

    val delegate1_InterfaceBase2: InterfaceBase2? = base.delegate
    assertEquals(base, delegate1_InterfaceBase2)
    val delegate2_InterfaceBase2: InterfaceBase2? = base.delegate()
    assertEquals(base, delegate2_InterfaceBase2)

    // value of `delegate()` should be zero initially
    val derived = InterfaceDerived2()
    val delegate4_Long: Long = derived.delegate()
    assertEquals(0L, delegate4_Long)

    // value of `delegate()` should be changed when `delegate: InterfaceBase2` is changed
    derived.delegate = derived
    val delegate3_InterfaceBase2: InterfaceBase2? = derived.delegate
    assertEquals(derived, delegate3_InterfaceBase2)
    val delegate5_Long: Long = derived.delegate()
    assertNotEquals(0L, delegate5_Long)
}
