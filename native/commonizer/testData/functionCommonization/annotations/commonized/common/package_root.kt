expect class Holder() {
    @Deprecated("This function is deprecated")
    expect fun deprecatedFunction1()

    @Deprecated("See concrete deprecation messages in actual declarations")
    expect fun deprecatedFunctionWithCustomizedAnnotation1()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation2()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation3()

    @Deprecated("This function is deprecated", level = DeprecationLevel.ERROR)
    expect fun deprecatedFunctionWithCustomizedAnnotation4()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation5()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation6()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation7()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation8()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation9()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation10()

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()"))
    expect fun deprecatedFunctionWithCustomizedAnnotation11()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation12()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation13()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation14()

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("", imports = arrayOf("org.sample.foo")))
    expect fun deprecatedFunctionWithCustomizedAnnotation15()

    @Deprecated("This function is deprecated")
    expect fun deprecatedFunctionWithCustomizedAnnotation16()

    @Deprecated("This function is deprecated", replaceWith = ReplaceWith("foo()", imports = arrayOf("org.sample.foo")))
    fun deprecatedFunctionWithCustomizedAnnotation17() {
    }

    @Deprecated("This function is deprecated")
    fun deprecatedFunctionWithCustomizedAnnotation18() {
    }

    @Deprecated("This function is deprecated")
    fun deprecatedFunctionWithCustomizedAnnotation19() {
    }

    expect fun nonDeprecatedFunction1()
}
