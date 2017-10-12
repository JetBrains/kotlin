
import org.gradle.jvm.tasks.Jar

description = "Kotlin Android Extensions Compiler"

apply { plugin("kotlin") }

dependencies {
    compileOnly(ideaSdkCoreDeps("intellij-core"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    runtime(projectRuntimeJar(":kotlin-compiler-embeddable"))
    compileOnly(commonDep("com.google.android", "android"))
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar: Jar by tasks
jar.apply {
    from(getSourceSetsFrom(":kotlin-android-extensions-runtime")["main"].output.classesDirs)
}

runtimeJar(rewriteDepsToShadedCompiler(jar))
sourcesJar()
javadocJar()

publish()
