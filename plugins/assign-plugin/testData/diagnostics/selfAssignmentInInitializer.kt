// ISSUE: KT-78987
annotation class ValueContainer

@ValueContainer
class StringProperty(var v: String) {
    fun assign(v: String) {
        this.v = v
    }
}

val topLevel = <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!><!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM, VAL_REASSIGNMENT!>topLevel<!> = "str"<!>

class Task {
    val member = <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!><!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>member<!> = "str"<!>

    fun usage() {
        member.toString()
    }
}
