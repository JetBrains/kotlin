
description = "Sample Kotlin JSR 223 scripting jar with daemon (out-of-process) compilation and local (in-process) evaluation"

apply { plugin("kotlin") }

val compilerClasspath by configurations.creating

dependencies {
    testCompile(project(":kotlin-stdlib"))
    testCompile(project(":kotlin-script-runtime"))
    testCompile(project(":kotlin-script-util"))
    testCompile(project(":kotlin-daemon-client"))
    testCompile(projectRuntimeJar(":kotlin-compiler-embeddable"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntime(project(":kotlin-reflect"))
    compilerClasspath(projectRuntimeJar(":kotlin-compiler-embeddable"))
    compilerClasspath(projectDist(":kotlin-reflect"))
    compilerClasspath(projectDist(":kotlin-stdlib"))
    compilerClasspath(projectDist(":kotlin-script-runtime"))
}

projectTest {
    doFirst {
        systemProperty("kotlin.compiler.classpath", compilerClasspath.asFileTree.asPath)
    }
}
