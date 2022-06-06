import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

annotation class A


fun @receiver:A Int.test() {

}

fun call() {
    42.test()
}