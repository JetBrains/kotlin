@file:DependsOn("@{kotlin-stdlib}")

fun main() {
    error("my error")
}

fun a() {
    main()
}

a()
