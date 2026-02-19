plugins {
    kotlin("jvm")
    kotlin("plugin.power-assert")
}

dependencies {
    testImplementation(kotlin("test"))
}

powerAssert {
    functions.addAll("kotlin.require")
    includedSourceSets.addAll("main")
}
