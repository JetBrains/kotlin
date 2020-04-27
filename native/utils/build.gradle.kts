plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native utils"

dependencies {
    compileOnly(kotlinStdlib())
    compile(project(":kotlin-util-io"))

    testCompile(commonDep("junit:junit"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntime(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

// TODO: this single known external consumer of this artifact is Kotlin/Native backend,
//  so publishing could be stopped after migration to monorepo
publish()

standardPublicJars()
