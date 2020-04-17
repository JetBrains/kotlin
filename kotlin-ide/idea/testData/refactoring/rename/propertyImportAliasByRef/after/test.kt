import Some.referWithoutAlias
import Some.referWithAli as referWithAlias

fun referRenamed() {
    println(referWithoutAlias)
    println(referWithAlias)
}

object Some {
    var referWithoutAlias = 0
    var referWithAli = 0
}