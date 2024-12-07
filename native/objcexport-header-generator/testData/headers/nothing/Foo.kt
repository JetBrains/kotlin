interface NothingCases {
    val nonNullVal: Nothing
    val nullVal: Nothing?

    var nonNullVar: Nothing
    var nullVar: Nothing?

    fun nonNullMethod(): Nothing
    fun nullMethod(): Nothing?
    fun nullParam(nothing: Nothing?)
    fun nonNullParam(nothing: Nothing)

    fun Nothing.nonNullExtension()
    fun Nothing.nullExtension()
}