class TestPropertyInitializer {
    var withSetter = "/sdcard"
        get() = field
        set(p) {
            field = p
        }
}
