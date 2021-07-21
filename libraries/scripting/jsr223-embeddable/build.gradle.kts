description = "Kotlin Scripting JSR-223 support"

plugins {
    java
}

dependencies {
    embedded(project(":kotlin-scripting-jsr223-unshaded")) { isTransitive = false }
    runtimeOnly(project(":kotlin-script-runtime"))
    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-scripting-common"))
    runtimeOnly(project(":kotlin-scripting-jvm"))
    runtimeOnly(project(":kotlin-scripting-jvm-host"))
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
