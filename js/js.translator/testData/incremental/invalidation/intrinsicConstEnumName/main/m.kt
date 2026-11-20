fun box(stepId: Int, isWasm: Boolean): String {
    val expected = if (stepId == 0) "OLD" else "NEW"
    if (LABEL != expected) return "Fail direct name: $LABEL"
    if (COPIED_LABEL != expected) return "Fail copied name: $COPIED_LABEL"
    val actual = labelFromLibrary()
    if (actual != expected) return "Fail indirect name: $actual"
    val stableMain = stableFromMain()
    if (stableMain != "STABLE") return "Fail stable main: $stableMain"
    val stableLibrary = stableFromLibrary()
    if (stableLibrary != "STABLE") return "Fail stable library: $stableLibrary"
    return "OK"
}
