plugins {
    kotlin("jvm")
}

val main by sourceSets.getting {
    dependencies {
        api(kotlinStdlib())
        implementation(project(":native:hair:sym"))
        implementation(project(":native:hair:utils"))
    }
}
