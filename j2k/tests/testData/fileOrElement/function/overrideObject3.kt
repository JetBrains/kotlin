// ERROR: This type is final, so it cannot be inherited from
class Base {
    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }
}

class X : Base() {
    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }
}