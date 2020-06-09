open class A<T, C, D<T>>

class B : A<Int, Long, List<Int>>(<caret>)

// SET_FALSE: ALIGN_MULTILINE_METHOD_BRACKETS