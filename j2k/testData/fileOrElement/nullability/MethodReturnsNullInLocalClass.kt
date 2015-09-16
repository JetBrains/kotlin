internal class C {
    fun foo(): String {
        internal class Local {
            fun foo(): String? {
                return null
            }
        }
        Local().foo()
        return ""
    }
}