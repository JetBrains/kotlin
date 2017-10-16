
description = "Annotation Processor for Kotlin"

apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:plugin-api"))
    testCompile(project(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    workingDir = rootDir
}

runtimeJar()
sourcesJar()
javadocJar()

dist()
