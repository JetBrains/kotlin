
plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":examples:scripting-jvm-maven-deps"))
    api(project(":kotlin-scripting-jvm-host-unshaded"))
    api(kotlinStdlib())
    compileOnly(project(":compiler:util"))

    testRuntimeOnly(project(":kotlin-compiler-embeddable"))
    testRuntimeOnly(project(":kotlin-scripting-compiler-embeddable"))

    testImplementation(libs.junit4)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
