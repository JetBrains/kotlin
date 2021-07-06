
plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":examples:scripting-jvm-simple-script"))
    compileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    testRuntimeOnly(project(":kotlin-compiler-embeddable"))
    testRuntimeOnly(project(":kotlin-scripting-compiler-embeddable"))
    testRuntimeOnly(project(":kotlin-scripting-jvm-host"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(intellijDep()) { includeJars("guava", rootProject = rootProject) }
    testApi(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

