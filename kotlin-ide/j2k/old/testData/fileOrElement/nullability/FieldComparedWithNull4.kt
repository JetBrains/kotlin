internal class C(private val s: String?) {

    init {
        if (s == null) {
            print("null")
        }
    }
}