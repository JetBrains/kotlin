
@file:DependsOn("junit:junit:4.11")

org.junit.Assert.assertTrue(true)

println("Hello, world!")

if (args.isNotEmpty()) {
    println(args.joinToString())
}

println("done")
