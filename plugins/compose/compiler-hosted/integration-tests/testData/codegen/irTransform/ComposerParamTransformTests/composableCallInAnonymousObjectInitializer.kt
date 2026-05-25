import androidx.compose.runtime.*

@Composable fun Test(inputs: List<Int>) {
    val objs = inputs.map {
        object {
            init {
                Foo()
            }

            val state = Foo()
            val value by Foo()
        }
    }
    objs.forEach {
        println(it.state)
        println(it.value)
    }
}
