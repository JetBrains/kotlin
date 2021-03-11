// IS_APPLICABLE: false

annotation class Ann

class AnnotatedParam<caret>(val v: Double, @Ann val x: Int, var s: String)