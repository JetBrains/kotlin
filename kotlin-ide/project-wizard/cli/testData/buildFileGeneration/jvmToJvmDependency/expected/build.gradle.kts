group = "testGroupId"
version = "1.0-SNAPSHOT"



allprojects {
    repositories {
        mavenCentral()
        maven {
            url = uri("KOTLIN_REPO")
        }
    }
}