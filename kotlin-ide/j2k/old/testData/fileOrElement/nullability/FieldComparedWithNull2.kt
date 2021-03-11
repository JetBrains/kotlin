internal class C(private val s: String?) {

    fun foo() {
        if (s != null) {
            print("not null")
        }
    }
}