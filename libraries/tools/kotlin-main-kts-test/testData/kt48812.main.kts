@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

fun printInt(n : Int) = println(n)

runBlocking {
    flowOf(1, 2, 3)
        .onEach(::printInt)
        .collect()
}
