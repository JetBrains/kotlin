fun box(stepId: Int, isWasm: Boolean): String {
    val expected = if (stepId == 0) "before" else "after"
    if (CALLABLE_NAME != expected) return "Fail direct callable: $CALLABLE_NAME"
    if (COPIED_CALLABLE_NAME != expected) return "Fail copied callable: $COPIED_CALLABLE_NAME"
    val actual = callableNameFromLibrary()
    if (actual != expected) return "Fail indirect callable: $actual"
    val stableMain = stableFromMain()
    if (stableMain != "STABLE") return "Fail stable main: $stableMain"
    val stableLibrary = stableFromLibrary()
    if (stableLibrary != "STABLE") return "Fail stable library: $stableLibrary"
    return "OK"
}
