plugins {
    kotlin("jvm")
}

val main by sourceSets.getting {
    dependencies {
        api(kotlinStdlib())
    }
}
val test by sourceSets.getting
