public trait INode {
    default object {
        public val IN: String = "in"
        public val AT: String = "@"
        public val COMMA_WITH_SPACE: String = COMMA + SPACE
    }
}