
plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":examples:scripting-jvm-maven-deps"))
    compile(project(":kotlin-scripting-jvm-host-unshaded"))
    compile(kotlinStdlib())
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(project(":compiler:util"))

    testRuntimeOnly(project(":kotlin-compiler"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":kotlin-scripting-compiler-unshaded"))

    testCompile(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
