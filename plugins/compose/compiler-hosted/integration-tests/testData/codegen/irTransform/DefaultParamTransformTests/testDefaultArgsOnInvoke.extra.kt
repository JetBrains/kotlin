import androidx.compose.runtime.*


object HasDefault {
    @Composable
    operator fun invoke(text: String = "SomeText"){
        println(text)
    }
}

object NoDefault {
    @Composable
    operator fun invoke(text: String){
        println(text)
    }
}

object MultipleDefault {
    @Composable
    operator fun invoke(text: String = "SomeText", value: Int = 5){
        println(text)
        println(value)
    }
}


fun used(x: Any?) {}
