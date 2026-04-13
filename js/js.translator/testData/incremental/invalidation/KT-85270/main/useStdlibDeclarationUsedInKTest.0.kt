fun useStdlibDeclarationUsedInKTest(): Boolean {
    // indexOf is used by the kotlin.test, which is loaded by backend context
    return "lol".indexOf('!') == -1
}