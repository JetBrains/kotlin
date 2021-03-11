// FIR_COMPARISON

class C {
    fun String.extFun() {
        <caret>
    }

    fun String.otherExtFun() { }
    fun Int.wrongExtFun() { }

    companion object {
        fun String.companionExtFun() { }
    }
}

fun String.nonMemberExtFun() {}

// EXIST: extFun
// EXIST: otherExtFun
// ABSENT: wrongExtFun
// EXIST: nonMemberExtFun
// EXIST: companionExtFun
