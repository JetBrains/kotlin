plugins {
    java
}

description = "Kotlin KLIB Library Commonizer (for using with embeddable compiler)"

dependencies {
    embedded(project(":native:kotlin-klib-commonizer")) { isTransitive = false }
    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-compiler-embeddable"))
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
emptySourcesJar()
emptyJavadocJar()
