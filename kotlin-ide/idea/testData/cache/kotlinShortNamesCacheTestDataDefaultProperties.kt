@file:JvmName("KotlinShortNameCacheTestData")

var topLevelVar = ""


object B1 {
    @JvmStatic
    var staticObjectVar = ""

    var nonStaticObjectVar = ""
}

class C1 {
    var classVar = ""

    companion object {
        @JvmStatic
        var staticCompanionVar = ""

        var nonStaticCompanionVar = ""
    }
}
