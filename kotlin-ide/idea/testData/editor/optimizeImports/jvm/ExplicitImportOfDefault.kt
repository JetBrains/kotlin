import java.lang.IllegalArgumentException
import kotlin.run

fun main(args: Array<String>) {
    run {
        throw IllegalArgumentException()
    }
}

fun type(): IllegalArgumentException? = null
