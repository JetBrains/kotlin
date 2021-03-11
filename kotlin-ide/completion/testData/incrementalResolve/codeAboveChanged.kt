fun foo(p1: Int, p2: String): Int {
    val v1 = p2<change>
    val v2 = 123
    return <before><caret>
}

// TYPE: ".hashCode()"
// COMPLETION_TYPE: SMART
// EXIST: v1
// EXIST: v2
// EXIST: p1
// ABSENT: p2
