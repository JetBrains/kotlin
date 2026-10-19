fun box(stepId: Int, isWasm: Boolean): String {
    val expectedName = if (stepId == 0) "before" else "after"
    if (CALLABLE_NAME != expectedName) return "Fail direct callable: $CALLABLE_NAME"
    if (COPIED_CALLABLE_NAME != expectedName) return "Fail copied callable: $COPIED_CALLABLE_NAME"
    val actualName = callableNameFromLibrary()
    if (actualName != expectedName) return "Fail indirect callable: $actualName"
    val stableMain = stableFromMain()
    if (stableMain != "STABLE") return "Fail stable main: $stableMain"
    val stableLibrary = stableFromLibrary()
    if (stableLibrary != "STABLE") return "Fail stable library: $stableLibrary"
    return "OK"
}
