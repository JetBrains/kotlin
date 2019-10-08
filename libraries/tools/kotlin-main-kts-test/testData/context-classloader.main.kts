
val klass = java.lang.Thread.currentThread().contextClassLoader.loadClass("org.jetbrains.kotlin.mainKts.MainKtsConfigurator")

println(klass.simpleName)