// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

class Context
open class LinearLayout(val context: Context)

abstract class Foo {}

abstract class Bar(context: Context) : LinearLayout(context) {}

@Composable fun Test() {
    <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Foo()<!>
    <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!><!NO_VALUE_FOR_PARAMETER!>Bar<!>()<!>
}
