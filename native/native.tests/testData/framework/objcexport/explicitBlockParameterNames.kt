fun callMyParametersExplicitlyPlease(cb: (a: Int, b: Int, c: String) -> String): String {
    return cb(5, 6, "hello")
}