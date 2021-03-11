import dependency.*
import dependency.J
import dependency.K
import dependency.impl.*
import dependency.impl.<error>JImpl</error>
import dependency.impl.<error>KImpl</error>

fun usage(): String {
    val j: J = J.getInstance()
    val k: K = K.getInstance()

    val jImpl: <error>JImpl</error> = J.getInstance()
    val kImpl: <error>KImpl</error> = K.getInstance()

    return "$j$k$jImpl$kImpl"
}
