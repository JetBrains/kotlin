// RUN_PIPELINE_TILL: FRONTEND

            import androidx.compose.runtime.*

            val lambda: @Composable (() -> Unit)? = null

            @Composable
            fun Foo() {
	        // Should fail as null cannot be coerced to non-null
                Bar(<!ARGUMENT_TYPE_MISMATCH!>lambda<!>)
                Bar(<!NULL_FOR_NONNULL_TYPE!>null<!>)
                Bar {}
            }

            @Composable
            fun Bar(child: @Composable () -> Unit) {
                child()
            }
