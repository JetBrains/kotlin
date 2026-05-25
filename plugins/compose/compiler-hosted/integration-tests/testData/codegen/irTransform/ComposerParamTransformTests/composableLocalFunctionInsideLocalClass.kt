import androidx.compose.runtime.*

fun test() {
    object: C() {
        @Composable
        override fun Render() {
            @Composable
            fun B() {
                Button({}) {}
            }

            B()
        }
    }
}
