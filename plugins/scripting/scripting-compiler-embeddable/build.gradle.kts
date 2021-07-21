description = "Kotlin Scripting Compiler Plugin for embeddable compiler"

plugins {
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
