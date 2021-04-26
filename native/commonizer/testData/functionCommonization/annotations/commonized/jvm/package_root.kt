actual class Holder actual constructor() {
    @Deprecated("This function is deprecated")
    actual fun deprecatedFunction1() {
    }

    @Deprecated("This function is deprecated")
    fun deprecatedFunction3() {
    }

    @Deprecated("This function is deprecated as well")
    actual fun deprecatedFunctionWithCustomizedAnnotation1() {
    }

    @Deprecated("This function is deprecated", level = DeprecationLevel.WARNING)
    actual fun deprecatedFunctionWithCustomizedAnnotation2() {
    }

    @Deprecated("This function is deprecated", level = DeprecationLevel.WARNING)
    actual fun deprecatedFunctionWithCustomizedAnnotation3() {
    }

    @Deprecated("This function is deprecated", level = DeprecationLevel.ERROR)
    actual fun deprecatedFunctionWithCustomizedAnnotation4() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith(""))
    actual fun deprecatedFunctionWithCustomizedAnnotation5() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith(""))
    actual fun deprecatedFunctionWithCustomizedAnnotation6() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = emptyArray()))
    actual fun deprecatedFunctionWithCustomizedAnnotation7() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = emptyArray()))
    actual fun deprecatedFunctionWithCustomizedAnnotation8() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = emptyArray()))
    actual fun deprecatedFunctionWithCustomizedAnnotation9() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()"))
    actual fun deprecatedFunctionWithCustomizedAnnotation10() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()"))
    actual fun deprecatedFunctionWithCustomizedAnnotation11() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()"))
    actual fun deprecatedFunctionWithCustomizedAnnotation12() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = arrayOf("org.sample.foo")))
    actual fun deprecatedFunctionWithCustomizedAnnotation13() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = arrayOf("org.sample.foo")))
    actual fun deprecatedFunctionWithCustomizedAnnotation14() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = arrayOf("org.sample.foo")))
    actual fun deprecatedFunctionWithCustomizedAnnotation15() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = arrayOf("org.sample.foo")))
    actual fun deprecatedFunctionWithCustomizedAnnotation16() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()", imports = arrayOf("org.sample.foo")))
    actual fun deprecatedFunctionWithCustomizedAnnotation17() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()", imports = arrayOf("org.sample.foo")))
    actual fun deprecatedFunctionWithCustomizedAnnotation18() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()", imports = arrayOf("org.sample.foo")))
    actual fun deprecatedFunctionWithCustomizedAnnotation19() {
    }

    actual fun nonDeprecatedFunction1() {}
    fun nonDeprecatedFunction3() {}
}
