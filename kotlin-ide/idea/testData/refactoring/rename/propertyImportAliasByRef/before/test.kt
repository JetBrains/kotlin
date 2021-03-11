import Some.referWithoutAlias
import Some.referWithAlias as referWithAlias

fun referRenamed() {
    println(referWithoutAlias)
    println(referWithAlias)
}

object Some {
    var referWithoutAlias = 0
    var referWithAlias/*rename*/ = 0
}