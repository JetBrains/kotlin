fun foo(actions: List<(String) -> Unit>) {
    actions[0](<caret>)
}

// EXIST: {"lookupString":"String","tailText":"() (kotlin)","itemText":"String"}
// EXIST: {"lookupString":"toString","tailText":"() (kotlin)","typeText":"String","itemText":"String.toString"}