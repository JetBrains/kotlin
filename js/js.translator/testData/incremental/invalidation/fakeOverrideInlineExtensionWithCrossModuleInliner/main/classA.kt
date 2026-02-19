class ClassA: AbstractClassA() {
    inline fun testExtension() = "testExtension".fakeOverrideExtension()
    inline fun testGetProperty() = "testGetProperty".fakeOverrideGetProperty

    inline fun testSetPropertySetter(): String {
        "test".fakeOverrideSetProperty = "testSetPropertySetter"
        return savedString;
    }

    inline fun testSetPropertyGetter() = "test".fakeOverrideSetProperty
}
