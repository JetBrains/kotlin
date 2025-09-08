gradle.beforeProject {
    // We could not use KGP symbols directly as the bootstrap repo is added
    // after transitive dependencies of this project should be resolved.
    // And the 'compileOnly' approach doesn't work because of the Gradle classload isolation.
    // Should be in sync with 'gradle-settings-conventions/gradle.properties'
    extensions
        .getByType(ExtraPropertiesExtension::class.java)
        // Should be in sync with settings in the Maven build in libraries/pom.xml#<properties>#<kotlin.compiler.daemon.jvmArgs>
        .set("kotlin.daemon.jvmargs", "-Xmx3g")
}

