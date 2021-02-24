plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
}

group = "org.sample.root"

allprojects {
    repositories {
        mavenLocal()
        jcenter()
    }
}

kotlin {
    linuxX64("linux")
    js {
        nodejs()
    }

    sourceSets["commonMain"].dependencies {
        implementation(kotlin("stdlib-common"))
        implementation(project(":foo"))
    }

    sourceSets["jsMain"].dependencies {
        implementation(kotlin("stdlib-js"))
    }
}
