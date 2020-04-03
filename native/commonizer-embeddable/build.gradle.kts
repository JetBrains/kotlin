description = "Kotlin KLIB Library Commonizer (for using with embeddable compiler)"

plugins {
    java
}

dependencies {
    embedded(project(":native:kotlin-klib-commonizer")) { isTransitive = false }
    runtime(kotlinStdlib())
    runtime(project(":kotlin-compiler-embeddable"))
}

sourceSets {
    "main" {}
    "test" {}
}

publish()

noDefaultJar()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJar()
javadocJar()
