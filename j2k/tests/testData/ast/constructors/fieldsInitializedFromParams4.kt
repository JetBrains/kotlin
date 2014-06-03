class C(p: Int, c: C) {
    public var p: Int = 0

    {
        c.p = p
    }
}