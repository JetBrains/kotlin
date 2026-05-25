// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

open class A {}
class B : A() {}

@Composable fun Foo(x: A = A(), y: A = B(), z: B = B()) {
    print(x)
    print(y)
    print(z)
}

@Composable fun Test() {
    Foo(
        x=A(),
        y=A(),
        z=<!ARGUMENT_TYPE_MISMATCH!>A()<!>
    )
    Foo(
        x=B(),
        y=B(),
        z=B()
    )
    Foo(
        x=<!ARGUMENT_TYPE_MISMATCH!>1<!>,
        y=<!ARGUMENT_TYPE_MISMATCH!>1<!>,
        z=<!ARGUMENT_TYPE_MISMATCH!>1<!>
    )
}
