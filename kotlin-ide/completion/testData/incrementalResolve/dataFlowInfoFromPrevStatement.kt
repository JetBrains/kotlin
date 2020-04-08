fun foo(p: Any): String {
    if (p !is String) return ""
    r<before><change>
}

// TYPE: "eturn "
// COMPLETION_TYPE: SMART
// EXIST: p
