fun call() {
    val list : String? = "sdfknsdkfm"
    "x" <error descr="[UNSAFE_OPERATOR_CALL] Operator call corresponds to a dot-qualified call 'list.contains(\"x\")' which is not allowed on a nullable receiver 'list'.">in</error> list

    "x" <error descr="[UNSAFE_OPERATOR_CALL] Operator call corresponds to a dot-qualified call 'list.contains(\"x\")' which is not allowed on a nullable receiver 'list'.">!in</error> list
}

<error>operator</error> fun CharSequence.contains(<warning>other</warning>: CharSequence, <warning>ignoreCase</warning>: Boolean = false): Boolean = true