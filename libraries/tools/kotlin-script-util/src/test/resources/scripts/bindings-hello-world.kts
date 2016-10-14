
println("Hello, world!")

if (bindings.isNotEmpty()) {
    println(bindings.entries.joinToString { "${it.key} = ${it.value}" })
}

println("done")
