
println("Hello, world!")

if (bindings.isNotEmpty()) {
    println(bindings.joinToString { "${it.key} = ${it.value}" })
}

println("done")
