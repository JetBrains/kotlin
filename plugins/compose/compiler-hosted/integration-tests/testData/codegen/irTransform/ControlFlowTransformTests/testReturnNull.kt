import androidx.compose.runtime.*

@Composable
fun Test(): String? {
    return null
}
@Composable
fun Test2(b: Boolean): String? {
    if (b) return "true"
    return null
}
@Composable
fun Test3(b: Boolean): String? {
    if (b) {
        return "true"
    } else {
        return null
    }
}
@Composable
fun Test4(b: Boolean): String? {
    return if (b) {
        "true"
    } else {
        null
    }
}
@Composable
fun Test5(): String? {
    var varNull = null
    return varNull
}
@Composable
fun Test6(): String? {
    TODO()
}
@Composable
fun Test7(b: Boolean): String? {
    if (b) {
        return null
    }
    return "false"
}
@Composable
fun Test8(): Unit? {
    var unitNull: Unit? = null
    Test6()
    return unitNull
}
@Composable
fun Test9(): Unit? {
    var unitNotNull: Unit? = Unit
    Test6()
    return unitNotNull
}
@Composable
fun Test10(): Unit? {
    Test6()
    return Unit
}
