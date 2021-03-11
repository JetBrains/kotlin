class Protected {
    internal var s: String

    protected <caret>constructor(s: String) {
        this.s = s
    }
}
