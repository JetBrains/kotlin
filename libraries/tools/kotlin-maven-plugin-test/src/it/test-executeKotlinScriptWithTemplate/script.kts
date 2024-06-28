import java.lang.Exception

if (!args.isEmpty())
    println("some args passed")

if (this !is kotlin.script.templates.standard.ScriptTemplateWithArgs)
    throw Exception("Unexpected script base class")

println("Hello from Kotlin script file!")