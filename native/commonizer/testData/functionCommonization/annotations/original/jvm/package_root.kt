@Deprecated("This function is deprecated")
fun deprecatedFunction1() {
}

@Deprecated("This function is deprecated")
fun deprecatedFunction3() {
}

class Holder {
    @Deprecated("This function is deprecated")
    fun deprecatedFunction1() {
    }

    @Deprecated("This function is deprecated")
    fun deprecatedFunction3() {
    }

    @Deprecated("This function is deprecated as well")
    fun deprecatedFunctionWithCustomizedAnnotation1() {
    }

    @Deprecated("This function is deprecated", level = DeprecationLevel.WARNING)
    fun deprecatedFunctionWithCustomizedAnnotation2() {
    }

    @Deprecated("This function is deprecated", level = DeprecationLevel.WARNING)
    fun deprecatedFunctionWithCustomizedAnnotation3() {
    }

    @Deprecated("This function is deprecated", level = DeprecationLevel.ERROR)
    fun deprecatedFunctionWithCustomizedAnnotation4() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith(""))
    fun deprecatedFunctionWithCustomizedAnnotation5() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith(""))
    fun deprecatedFunctionWithCustomizedAnnotation6() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = emptyArray()))
    fun deprecatedFunctionWithCustomizedAnnotation7() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = emptyArray()))
    fun deprecatedFunctionWithCustomizedAnnotation8() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = emptyArray()))
    fun deprecatedFunctionWithCustomizedAnnotation9() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()"))
    fun deprecatedFunctionWithCustomizedAnnotation10() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()"))
    fun deprecatedFunctionWithCustomizedAnnotation11() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()"))
    fun deprecatedFunctionWithCustomizedAnnotation12() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = arrayOf("org.sample.foo")))
    fun deprecatedFunctionWithCustomizedAnnotation13() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = arrayOf("org.sample.foo")))
    fun deprecatedFunctionWithCustomizedAnnotation14() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = arrayOf("org.sample.foo")))
    fun deprecatedFunctionWithCustomizedAnnotation15() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = arrayOf("org.sample.foo")))
    fun deprecatedFunctionWithCustomizedAnnotation16() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()", imports = arrayOf("org.sample.foo")))
    fun deprecatedFunctionWithCustomizedAnnotation17() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()", imports = arrayOf("org.sample.foo")))
    fun deprecatedFunctionWithCustomizedAnnotation18() {
    }

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()", imports = arrayOf("org.sample.foo")))
    fun deprecatedFunctionWithCustomizedAnnotation19() {
    }

    fun nonDeprecatedFunction1() {}
    fun nonDeprecatedFunction3() {}
}
