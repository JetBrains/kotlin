// "Safe delete 'useMe'" "false"
// ACTION: Convert function to property
// ACTION: Convert to block body
// ACTION: Create test
// ACTION: Specify return type explicitly

import useMe as used

fun <caret>useMe() = 0

fun foo() = used()