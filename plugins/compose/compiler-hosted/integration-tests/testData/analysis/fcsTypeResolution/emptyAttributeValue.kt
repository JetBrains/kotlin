// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable fun Foo(abc: Int, xyz: Int) {
    print(abc)
    print(xyz)
}

@Composable fun Test() {
    Foo(abc=<!SYNTAX!><!>)

    // NOTE(lmr): even though there is NO diagnostic here, there *is* a parse
    // error. This is intentional and done to mimic how kotlin handles function
    // calls with no value expression in a call parameter list (ie, `Foo(123,)`)
    Foo(abc=123, xyz=<!SYNTAX!><!>)
}
