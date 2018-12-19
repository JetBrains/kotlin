package kotlin.native.internal

class IntrinsicType {
    companion object {
        // Arithmetic
        const val PLUS                  = "PLUS"
        const val MINUS                 = "MINUS"
        const val TIMES                 = "TIMES"
        const val SIGNED_DIV            = "SIGNED_DIV"
        const val SIGNED_REM            = "SIGNED_REM"
        const val UNSIGNED_DIV          = "UNSIGNED_DIV"
        const val UNSIGNED_REM          = "UNSIGNED_REM"
        const val INC                   = "INC"
        const val DEC                   = "DEC"
        const val UNARY_PLUS            = "UNARY_PLUS"
        const val UNARY_MINUS           = "UNARY_MINUS"
        const val SHL                   = "SHL"
        const val SHR                   = "SHR"
        const val USHR                  = "USHR"
        const val AND                   = "AND"
        const val OR                    = "OR"
        const val XOR                   = "XOR"
        const val INV                   = "INV"
        const val SIGN_EXTEND           = "SIGN_EXTEND"
        const val ZERO_EXTEND           = "ZERO_EXTEND"
        const val INT_TRUNCATE          = "INT_TRUNCATE"
        const val FLOAT_TRUNCATE        = "FLOAT_TRUNCATE"
        const val FLOAT_EXTEND          = "FLOAT_EXTEND"
        const val SIGNED_TO_FLOAT       = "SIGNED_TO_FLOAT"
        const val UNSIGNED_TO_FLOAT     = "UNSIGNED_TO_FLOAT"
        const val FLOAT_TO_SIGNED       = "FLOAT_TO_SIGNED"
        const val SIGNED_COMPARE_TO     = "SIGNED_COMPARE_TO"
        const val UNSIGNED_COMPARE_TO   = "UNSIGNED_COMPARE_TO"
        const val NOT                   = "NOT"
        const val REINTERPRET           = "REINTERPRET"
        const val ARE_EQUAL_BY_VALUE    = "ARE_EQUAL_BY_VALUE"
        const val IEEE_754_EQUALS       = "IEEE_754_EQUALS"
        // ObjC related stuff
        const val OBJC_GET_MESSENGER            = "OBJC_GET_MESSENGER"
        const val OBJC_GET_MESSENGER_STRET      = "OBJC_GET_MESSENGER_STRET"
        const val OBJC_GET_OBJC_CLASS           = "OBJC_GET_OBJC_CLASS"
        const val OBJC_GET_RECEIVER_OR_SUPER    = "OBJC_GET_RECEIVER_OR_SUPER"

        // Other
        const val GET_CLASS_TYPE_INFO           = "GET_CLASS_TYPE_INFO"
        const val READ_BITS                     = "READ_BITS"
        const val WRITE_BITS                    = "WRITE_BITS"
        const val CREATE_UNINITIALIZED_INSTANCE = "CREATE_UNINITIALIZED_INSTANCE"
        const val LIST_OF_INTERNAL              = "LIST_OF_INTERNAL"
        const val IDENTITY                      = "IDENTITY"
        const val GET_CONTINUATION              = "GET_CONTINUATION"

        // Interop
        const val READ_PRIMITIVE        = "READ_PRIMITIVE"
        const val WRITE_PRIMITIVE       = "WRITE_PRIMITIVE"
        const val GET_POINTER_SIZE      = "GET_POINTER_SIZE"
        const val NATIVE_PTR_TO_LONG    = "NATIVE_PTR_TO_LONG"
        const val NATIVE_PTR_PLUS_LONG  = "NATIVE_PTR_PLUS_LONG"
        const val GET_NATIVE_NULL_PTR   = "GET_NATIVE_NULL_PTR"
    }
}