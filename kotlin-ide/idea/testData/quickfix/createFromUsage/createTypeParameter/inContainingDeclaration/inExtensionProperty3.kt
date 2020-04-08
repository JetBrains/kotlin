// "Create type parameter 'T' in property 'a'" "true"
val T.a: String
    get() {
        val b: T<caret>
        return ""
    }