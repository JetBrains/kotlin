description = "Kotlin Scripting JVM host (for using with embeddable compiler)"

plugins {
    java
}

dependencies {
    embedded(project(":kotlin-scripting-jvm-host-unshaded")) { isTransitive = false }
    runtimeOnly(project(":kotlin-script-runtime"))
    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-scripting-common"))
    runtimeOnly(project(":kotlin-scripting-jvm"))
    runtimeOnly(project(":kotlin-compiler-embeddable"))
    runtimeOnly(project(":kotlin-scripting-compiler-embeddable"))
}

sourceSets {
    "main" {}
    "test" {}
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJar()
javadocJar()
