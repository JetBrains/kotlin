// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'indexOfFirst{}'"
// IS_APPLICABLE_2: false
fun f8_indexOfFirst_complex(value: String, list: List<Any?>):Int{
    <caret>for ((i, any) in list.withIndex())
        if (any != null)
            if (any is String)
                if (any == value)
                    return i
    return -1
}