fun test() {
    val projectExtId: String? = "id"
    val s = "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:${projectExtId ?: "Kotlin_dev_Compiler"})"
}

// SET_INT: WRAP_ELVIS_EXPRESSIONS = 2