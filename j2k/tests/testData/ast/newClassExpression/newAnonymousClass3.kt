import kotlinApi.KotlinTrait

class C {
    fun foo() {
        val t = object : KotlinTrait {
            override fun nullableFun(): String? {
                return null
            }

            override fun notNullableFun(): String {
                return ""
            }

            override fun nonAbstractFun(): Int {
                return 0
            }
        }
    }
}