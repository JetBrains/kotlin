/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

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
