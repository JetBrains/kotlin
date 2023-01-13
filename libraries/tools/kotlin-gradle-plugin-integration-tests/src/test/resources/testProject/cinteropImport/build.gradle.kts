allprojects {
    group = "a"
    version = "1.0"

    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "build"
            url = rootProject.buildDir.resolve("repo").toURI()
        }
    }
}
