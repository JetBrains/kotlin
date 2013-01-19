package org.jetbrains.jet.j2k.ast



public open class Identifier(val name: String,
                             val myNullable: Boolean = true,
                             val quotingNeeded: Boolean = true): Expression() {
    public override fun isEmpty() = name.length() == 0

     private open fun ifNeedQuote(): String {
        if (quotingNeeded && (ONLY_KOTLIN_KEYWORDS.contains(name)) || name.contains("$")) {
            return quote(name)
        }

        return name
    }

    public override fun toKotlin(): String = ifNeedQuote()
    public override fun isNullable(): Boolean = myNullable

    class object {
        public val EMPTY_IDENTIFIER: Identifier = Identifier("")
        private open fun quote(str: String): String {
            return "`" + str + "`"
        }

        public val ONLY_KOTLIN_KEYWORDS: Set<String> = hashSet(
                "package", "as", "type", "val", "var", "fun", "is", "in", "object", "when", "trait", "This"
        );
    }
}
