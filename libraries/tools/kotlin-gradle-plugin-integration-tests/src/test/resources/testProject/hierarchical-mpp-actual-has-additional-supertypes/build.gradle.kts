plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    maven("<localRepo>")
    mavenCentral()
}

kotlin {
    jvm {}
    sourceSets {
        val common = maybeCreate("commonMain")
        val concurrent = maybeCreate("concurrentMain")
        val jvm = maybeCreate("jvmMain")

        concurrent.dependsOn(common)
        jvm.dependsOn(concurrent)
        jvm.dependsOn(common)
    }
}
