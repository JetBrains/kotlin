inline fun inlineGlobalVarGetWrapper() =  inlineGlobalVar

inline fun inlineGlobalVarSetWrapper(v: String) { inlineGlobalVar = v }

inline fun inlineGlobalVarWrapper(): String {
    return inlineGlobalVarGetWrapper()
}
