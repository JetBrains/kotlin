package test

fun main(args: Array<String>) {
    EnumTest.E<caret>
}

// EXIST: ENTRY1, ENTRY2
// ABSENT: EnumTest, EAnotherClass