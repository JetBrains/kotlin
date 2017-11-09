
println("should not be printed")

fun unlistedSectionName(body: () -> Unit): Unit = body()

let {
   println("Hello, World!")
}

println("ignore here")

unlistedSectionName {
   println("should not be printed too")
}

println("ignore here")

apply {
   println("That's all, folks!")
}

println("ignore here")
