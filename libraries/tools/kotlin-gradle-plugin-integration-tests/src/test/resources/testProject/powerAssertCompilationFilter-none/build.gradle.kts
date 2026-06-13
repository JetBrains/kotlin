plugins {
    kotlin("jvm")
    kotlin("plugin.power-assert")
}

dependencies {
    testImplementation(kotlin("test"))
}

powerAssert {
    addRuntimeDependency.set(false)
    functions.addAll("kotlin.require")
    compilationFilter.set({ false })
}
