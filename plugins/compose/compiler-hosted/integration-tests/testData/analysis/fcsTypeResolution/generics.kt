// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

class A { fun a() {} }
class B { fun b() {} }

@Composable fun <T> Bar(x: Int, value: T, f: (T) -> Unit) { println(value); println(f); println(x) }

@Composable fun Test() {

    val fa: (A) -> Unit = { it.a() }
    val fb: (B) -> Unit = { it.b() }

    Bar(x=1, value=A(), f={ it.a() })
    Bar(x=1, value=B(), f={ it.b() })
    Bar(x=1, value=A(), f=fa)
    Bar(x=1, value=B(), f=fb)
    Bar(x=1, value=B(), f={ it.<!UNRESOLVED_REFERENCE!>a<!>() })
    Bar(x=1, value=A(), f={ it.<!UNRESOLVED_REFERENCE!>b<!>() })
    Bar(
      x=1,
      value=A(),
      f=<!ARGUMENT_TYPE_MISMATCH!>fb<!>
    )
    Bar(
      x=1,
      value=B(),
      f=<!ARGUMENT_TYPE_MISMATCH!>fa<!>
    )
}
