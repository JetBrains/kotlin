package cases.special

public open class JvmFieldsClass {
    @JvmField
    public var publicField = "x"

    @JvmField
    internal var internalField = "y"

    @JvmField
    protected var protectedField = "y"

    public companion object JvmFieldsCompanion {
        @JvmField
        public var publicCField = "x"

        @JvmField
        internal var internalCField = "y"

        @JvmField
        protected var protectedCField = "y"

        public const val publicConst = 1
        internal const val internalConst = 2
        protected const val protectedConst = 3
        private const val privateConst = 4
    }
}

public object JvmFieldsObject {
    @JvmField
    public var publicField = "x"

    @JvmField
    internal var internalField = "y"

    public const val publicConst = 1
    internal const val internalConst = 2
    private const val privateConst = 4
}


@JvmField
public var publicField = "x"

@JvmField
internal var internalField = "y"

public const val publicConst = 1
internal const val internalConst = 2
private const val privateConst = 4
