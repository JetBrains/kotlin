interface I

class <caret>B(
        // INFO: {"checked": "true", "toAbstract": "true"}
        val s: String,
        // INFO: {"checked": "true", "toAbstract": "true"}
        val b: Boolean,
        val i: Int
) : I