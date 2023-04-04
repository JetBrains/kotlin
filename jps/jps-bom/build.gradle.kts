import plugins.signLibraryPublication

plugins {
    id("java-platform")
    id("maven-publish")

}
//
//group = "org.jetbrains.kotlin"
//version = "1.9.255-SNAPSHOT"
//
//repositories {
//    mavenCentral()
//}

dependencies {
    constraints {
        api(project(":jps:jps-plugin"))
        api(project(":jps:jps-common"))
        api(kotlinStdlib())
    }
//    testImplementation(platform("org.junit:junit-bom:5.9.1"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
}
configureCommonPublicationSettingsForGradle(signLibraryPublication)

publishing {
    publications {
        create<MavenPublication>("myPlatform") {
            from(components["javaPlatform"])
            pom {
                packaging = "pom"
            }
        }
    }
}

//tasks.test {
//    useJUnitPlatform()
//}