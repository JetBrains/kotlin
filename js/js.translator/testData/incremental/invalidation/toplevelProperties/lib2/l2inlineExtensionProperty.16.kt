inline fun Int.inlineExtensionPropertyGetWrapper() = this.inlineExtensionProperty

inline fun Int.inlineExtensionPropertySetWrapper(v: String) { this.inlineExtensionProperty = v }

inline fun Int.inlineExtensionPropertyWrapper(): String {
    return this.inlineExtensionPropertyGetWrapper()
}
