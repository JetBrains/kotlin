package hair.sym

enum class HairType {
    VOID, // FIXME Unit?
    //BOOLEAN, BYTE, SHORT,
    INT, LONG, FLOAT, DOUBLE, REFERENCE,
    // TODO i128?
    EXCEPTION;

    val isIntegral get() = when (this) {
        INT,
        LONG -> true
        FLOAT,
        DOUBLE -> false
        else -> error("Should not reach here $this")
    }
}
