plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    api(project(":kotlin-script-runtime"))
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-jvm"))
    implementation(kotlinStdlib())
    implementation(project(":compiler:util"))

}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}


runtimeJar()
