
plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":examples:scripting-jvm-simple-script"))
    compile(project(":kotlin-scripting-jvm-host"))
    compile(project(":kotlin-script-util"))
    testRuntimeOnly(project(":kotlin-compiler"))
    testRuntimeOnly(project(":kotlin-scripting-compiler"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testCompile(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
