
description = "Sample Kotlin JSR 223 scripting jar with local (in-process) compilation and evaluation"

apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(projectDist(":kotlin-script-runtime"))
    compile(projectRuntimeJar(":kotlin-compiler"))
    compile(project(":kotlin-script-util"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testRuntime(projectDist(":kotlin-reflect"))
}

projectTest()
