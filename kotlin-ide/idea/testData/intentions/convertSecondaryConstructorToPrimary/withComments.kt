class WithComments {
    /**
     * A very important property
     */
    val veryImportant: Any

    /**
     * A constructor
     */
    constructor<caret>(/* Some parameter */veryImportant: Any) {
        this.veryImportant = veryImportant
    }
}