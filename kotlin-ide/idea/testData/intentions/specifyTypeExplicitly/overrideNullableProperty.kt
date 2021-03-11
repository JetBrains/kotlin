// CHOOSE_NULLABLE_TYPE_IF_EXISTS
// WITH_RUNTIME
interface Base {
    val nullable: String?
}

class Tesst : Base {
    override val nullable<caret> = java.lang.String.valueOf("")
}
