@file:DependsOn("@{kotlin-stdlib}")

import org.jetbrains.kotlin.scripting.compiler.test.DependsOn

fun main() {
    error("my error")
}

fun a() {
    main()
}

a()
