description = "Kotlin Scripting Compiler Plugin for embeddable compiler"

plugins {
    id("java-instrumentation")
    java
}

dependencies {
    embedded(project(":kotlin-scripting-compiler")) { isTransitive = false }
    runtimeOnly(project(":kotlin-scripting-compiler-impl-embeddable"))
    runtimeOnly(kotlinStdlib())
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()
javadocJar()
