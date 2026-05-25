// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable fun Fam(bar: Int, x: Int) {
    print(bar)
    print(x)
}

@Composable fun Test() {
    Fam(
      bar=<!UNRESOLVED_REFERENCE!>undefined<!>,
      x=1
    )
    Fam(
      bar=1,
      x=<!UNRESOLVED_REFERENCE!>undefined<!>
    )
    Fam(
      <!UNRESOLVED_REFERENCE!>bar<!>,
      <!UNRESOLVED_REFERENCE!>x<!>
    )

    Fam(
      bar=<!ARGUMENT_TYPE_MISMATCH!>""<!>,
      x=<!ARGUMENT_TYPE_MISMATCH!>""<!>
    )
}
