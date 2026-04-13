fun box(stepId: Int, isWasm: Boolean): String {
    return if (useStdlibDeclarationUsedInKTest()) "OK" else "FAIL"
}