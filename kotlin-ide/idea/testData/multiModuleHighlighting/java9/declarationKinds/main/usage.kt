import unexported.*

fun usage(): String {
    val k: <error>Klass</error> = <error>Klass</error>()
    val i: <error>Interface</error>? = null

    val ta1: <error>TypeAliasToPublic</error> = <error>TypeAliasToPublic</error>()
    val ta2: <error>TypeAliasToUnexported</error> = <error>TypeAliasToUnexported</error>()

    <error>function</error>()

    <error>valProperty</error>
    <error>varProperty</error>
    <error>varProperty</error> = ""

    return "$k$i$ta1$ta2"
}
