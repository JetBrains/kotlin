plugins {
    kotlin("multiplatform")
}

group = "kgp.it"
version = "0.0.1"

kotlin {
    jvm()

    js { browser() }
}
