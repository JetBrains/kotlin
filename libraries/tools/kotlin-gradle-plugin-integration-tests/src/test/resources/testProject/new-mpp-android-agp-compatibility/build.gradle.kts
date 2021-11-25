allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

tasks.register<Delete>("clean") {
    delete(buildDir)
}
