package hair.sym

enum class HairType {
    VOID, // FIXME Unit?
    //BOOLEAN, BYTE, SHORT,
    INT, LONG, FLOAT, DOUBLE, REFERENCE,
    NATIVE_POINTER,
    // TODO i128?
    EXCEPTION;

    val isIntegral get() = when (this) {
        INT,
        LONG -> true
        FLOAT,
        DOUBLE -> false
        REFERENCE,
        NATIVE_POINTER -> true

        VOID, EXCEPTION -> error("Should not reach here $this")
    }
}
