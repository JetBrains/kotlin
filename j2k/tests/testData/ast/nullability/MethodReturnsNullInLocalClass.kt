class C {
    fun foo(): String {
        class Local {
            fun foo(): String? {
                return null
            }
        }
        Local().foo()
        return ""
    }
}