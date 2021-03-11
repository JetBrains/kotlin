fun foo(p1: Int, p2: String): String {
    r<before><change>
}

// TYPE: "eturn "
// COMPLETION_TYPE: SMART
// ABSENT: p1
// EXIST: p2
