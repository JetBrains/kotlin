description = "Kotlin Scripting Compiler Plugin for embeddable compiler"

plugins {
    `java-library`
}

dependencies {
    embedded(project(":kotlin-scripting-compiler")) { isTransitive = false }
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-jvm"))
    api(project(":kotlin-scripting-compiler-impl-embeddable"))
    runtimeOnly(kotlinStdlib())
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()
javadocJar()
