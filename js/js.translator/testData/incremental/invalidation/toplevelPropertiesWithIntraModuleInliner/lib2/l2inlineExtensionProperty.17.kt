inline fun Int.inlineExtensionPropertyGetWrapper() = this.inlineExtensionProperty

inline fun Int.inlineExtensionPropertySetWrapper(v: String) { this.inlineExtensionProperty = v }

inline fun Int.inlineExtensionPropertyWrapper(): String {
    this.inlineExtensionPropertySetWrapper("1")
    return this.inlineExtensionPropertyGetWrapper()
}
