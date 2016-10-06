import java.lang.Exception

if (!args.isEmpty())
    println("some args passed")

if (this !is org.jetbrains.kotlin.script.util.StandardScript)
    throw Exception("Unexpected script base class")

println("Hello from Kotlin script file!")