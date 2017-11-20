
description = "Kotlin SourceSections Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.script"))
    compileOnly(project(":compiler:plugin-api"))
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:frontend.script"))
    testCompile(project(":compiler:plugin-api"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":compiler:daemon-common"))
    testCompile(project(":kotlin-daemon-client"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(ideaSdkDeps("idea", "idea_rt", "openapi"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

runtimeJar()
sourcesJar()
javadocJar()

dist()

publish()
