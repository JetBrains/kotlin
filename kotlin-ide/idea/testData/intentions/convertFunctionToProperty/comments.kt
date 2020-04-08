annotation class X(val s: String)

// 1
@X("") // 2
/* 3 */ fun fo<caret>o(): String {
    // 4
    return ""
}