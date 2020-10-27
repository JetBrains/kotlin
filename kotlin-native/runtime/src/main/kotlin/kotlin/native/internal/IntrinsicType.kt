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
        const val EXTRACT_ELEMENT       = "EXTRACT_ELEMENT"
        const val ARE_EQUAL_BY_VALUE    = "ARE_EQUAL_BY_VALUE"
        const val IEEE_754_EQUALS       = "IEEE_754_EQUALS"

        // ObjC related stuff
        const val OBJC_GET_MESSENGER            = "OBJC_GET_MESSENGER"
        const val OBJC_GET_MESSENGER_STRET      = "OBJC_GET_MESSENGER_STRET"
        const val OBJC_GET_OBJC_CLASS           = "OBJC_GET_OBJC_CLASS"
        const val OBJC_CREATE_SUPER_STRUCT      = "OBJC_CREATE_SUPER_STRUCT"
        const val OBJC_INIT_BY                  = "OBJC_INIT_BY"
        const val OBJC_GET_SELECTOR             = "OBJC_GET_SELECTOR"

        // Other
        const val GET_CLASS_TYPE_INFO           = "GET_CLASS_TYPE_INFO"
        const val INTEROP_READ_BITS             = "INTEROP_READ_BITS"
        const val INTEROP_WRITE_BITS            = "INTEROP_WRITE_BITS"
        const val CREATE_UNINITIALIZED_INSTANCE = "CREATE_UNINITIALIZED_INSTANCE"
        const val LIST_OF_INTERNAL              = "LIST_OF_INTERNAL"
        const val IDENTITY                      = "IDENTITY"
        const val IMMUTABLE_BLOB                = "IMMUTABLE_BLOB"
        const val INIT_INSTANCE                 = "INIT_INSTANCE"

        // Enums
        const val ENUM_VALUES                   = "ENUM_VALUES"
        const val ENUM_VALUE_OF                 = "ENUM_VALUE_OF"

        // Coroutines
        const val GET_CONTINUATION              = "GET_CONTINUATION"
        const val RETURN_IF_SUSPENDED           = "RETURN_IF_SUSPENDED"
        const val COROUTINE_LAUNCHPAD           = "COROUTINE_LAUNCHPAD"

        // Interop
        const val INTEROP_READ_PRIMITIVE        = "INTEROP_READ_PRIMITIVE"
        const val INTEROP_WRITE_PRIMITIVE       = "INTEROP_WRITE_PRIMITIVE"
        const val INTEROP_GET_POINTER_SIZE      = "INTEROP_GET_POINTER_SIZE"
        const val INTEROP_NATIVE_PTR_TO_LONG    = "INTEROP_NATIVE_PTR_TO_LONG"
        const val INTEROP_NATIVE_PTR_PLUS_LONG  = "INTEROP_NATIVE_PTR_PLUS_LONG"
        const val INTEROP_GET_NATIVE_NULL_PTR   = "INTEROP_GET_NATIVE_NULL_PTR"
        const val INTEROP_CONVERT               = "INTEROP_CONVERT"
        const val INTEROP_BITS_TO_FLOAT         = "INTEROP_BITS_TO_FLOAT"
        const val INTEROP_BITS_TO_DOUBLE        = "INTEROP_BITS_TO_DOUBLE"
        const val INTEROP_SIGN_EXTEND           = "INTEROP_SIGN_EXTEND"
        const val INTEROP_NARROW                = "INTEROP_NARROW"
        const val INTEROP_STATIC_C_FUNCTION     = "INTEROP_STATIC_C_FUNCTION"
        const val INTEROP_FUNPTR_INVOKE         = "INTEROP_FUNPTR_INVOKE"

        // Worker
        const val WORKER_EXECUTE                = "WORKER_EXECUTE"
    }
}