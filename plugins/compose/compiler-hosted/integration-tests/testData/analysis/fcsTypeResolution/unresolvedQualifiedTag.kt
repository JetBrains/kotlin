// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

object MyNamespace {
    @Composable fun Bar(content: @Composable () -> Unit = {}) {
        content()
    }

    var Baz = @Composable { }

    var someString = ""
    class NonComponent {}
}

class Boo {
    @Composable fun Wat() { }
}

@Composable fun Test() {

    MyNamespace.Bar()
    MyNamespace.Baz()
    MyNamespace.<!UNRESOLVED_REFERENCE!>Qoo<!>()
    MyNamespace.<!FUNCTION_EXPECTED!>someString<!>()
    MyNamespace.NonComponent()
    MyNamespace.Bar {}
    MyNamespace.Baz <!TOO_MANY_ARGUMENTS!>{}<!>

    val obj = Boo()
    Boo.<!UNRESOLVED_REFERENCE!>Wat<!>()
    obj.Wat()

    MyNamespace.<!UNRESOLVED_REFERENCE!>Bam<!>()
    <!UNRESOLVED_REFERENCE!>SomethingThatDoesntExist<!>.Foo()

    obj.Wat <!TOO_MANY_ARGUMENTS!>{
    }<!>

    MyNamespace.<!UNRESOLVED_REFERENCE!>Qoo<!> {
    }

    MyNamespace.<!FUNCTION_EXPECTED!>someString<!> {
    }

    <!UNRESOLVED_REFERENCE!>SomethingThatDoesntExist<!>.Foo {
    }

    MyNamespace.NonComponent <!TOO_MANY_ARGUMENTS!>{}<!>

    MyNamespace.<!UNRESOLVED_REFERENCE!>Bam<!> {}

}
