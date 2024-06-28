import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testOverrideInit1() {
    assertEquals(42, (TestOverrideInitImpl1.createWithValue(42) as TestOverrideInitImpl1).value)
}

private class TestOverrideInitImpl1 @OverrideInit constructor(val value: Int) : TestOverrideInit(value) {
    companion object : TestOverrideInitMeta()
}

// See https://youtrack.jetbrains.com/issue/KT-41910
@Test fun testOverrideInitWithDefaultArguments() {
    assertEquals(42, (TestOverrideInitImpl2.createWithValue(42) as TestOverrideInitImpl2).value)
    assertEquals(123, TestOverrideInitImpl2(123).value)
    assertEquals(17, TestOverrideInitImpl2().value)
}

private class TestOverrideInitImpl2 @OverrideInit constructor(val value: Int = 17) : TestOverrideInit(value) {
    companion object : TestOverrideInitMeta()
}
