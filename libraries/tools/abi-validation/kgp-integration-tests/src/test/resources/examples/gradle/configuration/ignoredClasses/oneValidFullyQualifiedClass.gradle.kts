kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        filters.exclude.byNames.add("com.company.BuildConfig")
    }
}
