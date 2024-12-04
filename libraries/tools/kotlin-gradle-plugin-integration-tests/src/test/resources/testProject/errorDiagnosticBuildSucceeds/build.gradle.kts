plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    // no targets declared -- will provoke ERROR-diagnostic, but also no compileKotlin tasks will be created,
    // so 'assemble' or similar will pass
}

tasks.create("myTask") {
    println("Custom Task Executed")
}
