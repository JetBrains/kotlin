class ClassA: AbstractClassA() {
    inline fun testPropertyWithGetter() = propertyWithGetter

    inline fun testPropertyWithSetter(): String {
        propertyWithSetter = "test"
        return savedStr
    }
}
