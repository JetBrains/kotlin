import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testInitWithCustomSelector() {
    assertFalse(TestInitWithCustomSelector().custom)
    assertTrue(TestInitWithCustomSelector(custom = Unit).custom)

    val customSubclass: TestInitWithCustomSelector = TestInitWithCustomSelectorSubclass.createCustom()
    assertTrue(customSubclass is TestInitWithCustomSelectorSubclass)
    assertTrue(customSubclass.custom)

    // Test side effect:
    var ok = false
    assertTrue(TestInitWithCustomSelector(run { ok = true }).custom)
    assertTrue(ok)
}

private class TestInitWithCustomSelectorSubclass : TestInitWithCustomSelector {
    @OverrideInit constructor(custom: Unit) : super(custom) {
        assertSame(Unit, custom)
    }

    companion object : TestInitWithCustomSelectorMeta()
}