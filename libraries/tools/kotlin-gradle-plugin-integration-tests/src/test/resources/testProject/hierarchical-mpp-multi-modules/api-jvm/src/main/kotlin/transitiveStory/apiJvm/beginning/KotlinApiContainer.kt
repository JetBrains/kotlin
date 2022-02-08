package transitiveStory.apiJvm.beginning

import playground.moduleName

open class KotlinApiContainer {
    private val privateKotlinDeclaration = "I'm a private Kotlin string from `" + moduleName +
            "` and shall be never visible to the others."

    internal val packageVisibleKotlinDeclaration = "I'm a package visible Kotlin string from `" + moduleName +
            "` and shall be never visible to the other modules."

    protected open val protectedKotlinDeclaration = "I'm a protected Kotlin string from `" + moduleName +
            "` and shall be never visible to the other modules except my subclasses."

    val publicKotlinDeclaration = "I'm a public Kotlin string from `" + moduleName +
            "` and shall be visible to the other modules."

    companion object {
        val publicStaticKotlinDeclaration = "I'm a public Kotlin static string from `" + moduleName +
                "` and shall be visible to the other modules even without instantiation of `JavaApiContainer` class."
    }
}

val tlAPIval = 42
