fun willBeResolvedToOther() {
    fun JavaClass.f(s: String) {
    }

    JavaClass().f(":|")
}