
description = "Sample Kotlin JSR 223 scripting jar with daemon (out-of-process) compilation and local (in-process) evaluation"

apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(projectDist(":kotlin-script-runtime"))
    compile(projectRuntimeJar(":kotlin-compiler"))
    compile(project(":kotlin-script-util"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testRuntime(projectDist(":kotlin-reflect"))
}

projectTest()
