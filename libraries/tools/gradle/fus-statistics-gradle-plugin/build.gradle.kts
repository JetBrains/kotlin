plugins {
    id("gradle-plugin-common-configuration")
    //configuration should be done in Project.configureCommonPublicationSettingsForGradle
    `maven-publish`
}


dependencies {
    commonApi(project(":kotlin-gradle-plugin-api"))
    commonApi(project(":kotlin-gradle-plugin"))
}

publishing {
    publications {
        create<MavenPublication>("Main") {
            from(components["java"])
        }
    }
}

