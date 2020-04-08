fun String.foo() {
    val v = ::xxx_<caret>
}

// EXIST: xxx_topLevelFun
// EXIST: xxx_topLevelVal
// ABSENT: xxx_extFun
