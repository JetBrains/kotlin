// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
interface Base {
    fun compose(content: () -> Unit)
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Impl<!> : Base {
    <!NOTHING_TO_OVERRIDE!>override<!> fun compose(content: @Composable () -> Unit) {}
}
