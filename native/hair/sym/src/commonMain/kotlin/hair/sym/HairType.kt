package hair.sym

enum class HairType {
    VOID,
    NOTHING,
    BOOLEAN,
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    REFERENCE,
    NATIVE_POINTER,
    // TODO i128?
    ;

    val isIntegral
        get() = when (this) {
            BOOLEAN -> true

            BYTE,
            SHORT,
            INT,
            LONG -> true

            FLOAT,
            DOUBLE -> false

            REFERENCE,
            NATIVE_POINTER -> true

            NOTHING,
            VOID -> error("Should not reach here $this")
        }
}
