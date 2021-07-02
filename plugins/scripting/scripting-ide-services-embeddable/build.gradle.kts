description = "Kotlin Scripting Compiler extension providing code completion and static analysis (for using in embeddable mode)"

plugins { java }

dependencies {
    embedded(project(":kotlin-scripting-ide-services-unshaded")) { isTransitive = false }
    embedded(project(":kotlin-scripting-ide-common")) { isTransitive = false }
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

noDefaultJar()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJar()
javadocJar()
