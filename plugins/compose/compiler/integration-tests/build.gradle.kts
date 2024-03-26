plugins {
    id("kotlin")
}

description = "Contains test for the compose compiler daemon"

dependencies {
    testImplementation(project(":kotlin-stdlib"))
    testImplementation(project(":compiler:cli-common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit4)
    testImplementation(gradleTestKit())
}

projectTest(parallel = false) {
    dependsOn(":plugins:compose-compiler-plugin:temp:compiler:publish")
    workingDir = rootDir
}