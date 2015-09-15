internal class C {
    internal fun foo(): String {
        internal class Local {
            internal fun foo(): String? {
                return null
            }
        }
        Local().foo()
        return ""
    }
}