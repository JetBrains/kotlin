inline fun globalVarGetWrapper() =  globalVar

inline fun globalVarSetWrapper(v: String) { globalVar = v }

inline fun globalVarWrapper(): String {
    globalVarSetWrapper("2")
    return globalVarGetWrapper()
}
