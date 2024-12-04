
@file:Import("import-common.main.kts")
@file:Import("import-middle.main.kts")

sharedVar = sharedVar + 1

class CapturingClass1 {
    val value = sharedVar
}

class CapturingClass2 {
    fun f() = CapturingClass1().value
}

println("${SharedObject.greeting} ${from.msg} main")
println("sharedVar == ${CapturingClass2().f()}")
