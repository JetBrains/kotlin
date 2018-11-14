
plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":examples:scripting-jvm-simple-script"))
    compileOnly(project(":kotlin-scripting-jvm-host"))
    compile(project(":kotlin-script-util"))
    testRuntimeOnly(projectRuntimeJar(":kotlin-compiler-embeddable"))
    testRuntimeOnly(project(":kotlin-scripting-jvm-host-embeddable"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(intellijDep()) { includeJars("guava", rootProject = rootProject) }
    testCompile(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

