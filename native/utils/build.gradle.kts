plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native utils"

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-util-io"))

    testCompile(commonDep("junit:junit"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntime(project(":kotlin-reflect"))}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

standardPublicJars()

