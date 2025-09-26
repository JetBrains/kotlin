package hair.sym

sealed interface Type {
    interface Reference : Type
    enum class Primitive : Type {
        BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, POINTER, VECTOR128
    }
}