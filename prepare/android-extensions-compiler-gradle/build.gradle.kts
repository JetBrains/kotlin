
import org.gradle.jvm.tasks.Jar

description = "Kotlin Android Extensions Compiler"

apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    runtime(projectRuntimeJar(":kotlin-compiler-embeddable"))
    compileOnly(commonDep("com.google.android", "android"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellijCoreJar())
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

// fixes a deadlock in projects evaluation order for :plugins:android-extensions-compiler
evaluationDependsOn(":plugins")

val jar: Jar by tasks
jar.apply {
    from(getSourceSetsFrom(":plugins:android-extensions-compiler")["main"].output)
    from(getSourceSetsFrom(":kotlin-android-extensions-runtime")["main"].output)
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

runtimeJar(rewriteDepsToShadedCompiler(jar))
sourcesJar()
javadocJar()

publish()
