package hair.sym

enum class HairType {
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
            HairType.BOOLEAN -> true

            HairType.BYTE,
            HairType.SHORT,
            HairType.INT,
            HairType.LONG -> true

            HairType.FLOAT,
            HairType.DOUBLE -> false

            HairType.REFERENCE,
            HairType.NATIVE_POINTER -> true

            HairType.NOTHING -> error("Should not reach here $this")
        }
}
