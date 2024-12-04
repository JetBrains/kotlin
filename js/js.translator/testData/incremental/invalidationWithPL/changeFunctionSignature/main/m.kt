fun box(stepId: Int, isWasm: Boolean): String {
    val x = test()
    if (stepId == 4) {
        // this is known issue, that return type doesn not affect IdSignature
        // https://youtrack.jetbrains.com/issue/KT-51707
        if (isWasm) {
            // The Wasm version doesn't change return type because
            //  the validation phase won't let the test pass.
            if (x != 5) return "Fail; got $x"
        } else {
            if (x != 41) return "Fail; got $x"
        }
    } else {
        if (stepId != x) return "Fail; got $x"
    }
    return "OK"
}
