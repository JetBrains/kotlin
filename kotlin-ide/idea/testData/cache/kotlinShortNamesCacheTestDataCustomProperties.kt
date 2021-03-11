@file:JvmName("KotlinShortNameCacheTestData")

var topLevelVar: String
    get() {
        return ""
    }
    set(value) {
    }


object B1 {
    @JvmStatic
    var staticObjectVar: String
        get() {
            return ""
        }
        set(value) {
        }

    var nonStaticObjectVar: String
        get() {
            return ""
        }
        set(value) {
        }
}

class C1 {
    var classVar: String
        get() {
            return ""
        }
        set(value) {
        }

    companion object {
        @JvmStatic
        var staticCompanionVar: String
            get() {
                return ""
            }
            set(value) {
            }

        var nonStaticCompanionVar: String
            get() {
                return ""
            }
            set(value) {
            }
    }
}
