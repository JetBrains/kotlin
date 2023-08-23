
plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":examples:scripting-jvm-simple-script"))
    compileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    testRuntimeOnly(project(":kotlin-compiler-embeddable"))
    testRuntimeOnly(project(":kotlin-scripting-compiler-embeddable"))
    testRuntimeOnly(project(":kotlin-scripting-jvm-host"))
    testRuntimeOnly(libs.guava)
    testImplementation(libs.junit4)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

