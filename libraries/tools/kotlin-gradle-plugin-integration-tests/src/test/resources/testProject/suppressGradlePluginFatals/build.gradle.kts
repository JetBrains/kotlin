plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        val intermediate = create("intermediate")

        val jvmMain = getByName("jvmMain") {
            dependsOn(intermediate)
        }

        intermediate.dependsOn(jvmMain)
    }
}