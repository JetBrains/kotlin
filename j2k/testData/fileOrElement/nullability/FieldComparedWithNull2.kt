internal class C(private val s: String?) {

    internal fun foo() {
        if (s != null) {
            print("not null")
        }
    }
}