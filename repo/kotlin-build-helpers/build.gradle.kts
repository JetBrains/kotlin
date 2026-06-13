plugins {
    `embedded-kotlin`
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.jetbrains.kotlin"

repositories {
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
}

dependencies {
    compileOnly(kotlin("stdlib"))
}

kotlin.jvmToolchain(17)

sourceSets {
    main {
        java.srcDirs("src")
    }
}

gradlePlugin {
    plugins.create("kotlinBuildHelpers") {
        id = "kotlin-build-helpers"
        implementationClass = "KotlinBuildHelpersPlugin"
    }
}
