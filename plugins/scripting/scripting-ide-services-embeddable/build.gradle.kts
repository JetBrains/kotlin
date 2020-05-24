description = "Kotlin Scripting Compiler extension providing code completion and static analysis (for using in embeddable mode)"

plugins { java }

dependencies {
    embedded(project(":kotlin-scripting-ide-services-unshaded")) { isTransitive = false }
    embedded(project(":idea:ide-common")) { isTransitive = false }
    runtime(project(":kotlin-script-runtime"))
    runtime(kotlinStdlib())
    runtime(project(":kotlin-scripting-common"))
    runtime(project(":kotlin-scripting-jvm"))
    runtime(project(":kotlin-compiler-embeddable"))
    runtime(project(":kotlin-scripting-compiler"))
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
