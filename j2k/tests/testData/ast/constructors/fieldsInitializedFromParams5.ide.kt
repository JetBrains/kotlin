class C(p: Int) {
    public var p: Int = 0

    {
        this.p = 0
        if (p > 0) {
            this.p = p
        }
    }
}