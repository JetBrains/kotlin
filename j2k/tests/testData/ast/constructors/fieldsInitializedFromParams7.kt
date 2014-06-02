open class C(x: Any?, b: Boolean) {
    public var x: Any? = null

    {
        if (b) {
            this.x = x
        }
    }
}
