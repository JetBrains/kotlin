// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable fun Foo(x: Int = 1) { print(x) }

@Composable fun Test() {
    Foo(
        x=<!UNRESOLVED_REFERENCE!>someUnresolvedValue<!>,
        <!NAMED_PARAMETER_NOT_FOUND!>y<!>=<!UNRESOLVED_REFERENCE!>someUnresolvedValue<!>
    )
}
