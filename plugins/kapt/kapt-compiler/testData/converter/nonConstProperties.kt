import ConstReferences.Consts

object Literals {
    val booleanFalse: Boolean = false
    val booleanTrue: Boolean = true

    val int11: Int = 11
    val byte12: Byte = 12.toByte()
    val short13: Short = 13.toShort()
    val charC: Char = 'C'
    val long14: Long = 14L
    val float15: Float = 1.5f
    val double16: Double = 1.6

    val uint21: UInt = 21U
    val ubyte22: UByte = 22.toUByte()
    val ushort23: UShort = 23.toUShort()
    val ulong24: ULong = 24UL

    val string: String = "Hello, world!"
}

object ConstReferences {
    val booleanFalse: Boolean = Consts.booleanFalse
    val booleanTrue: Boolean = Consts.booleanTrue

    val int11: Int = Consts.int11
    val byte12: Byte = Consts.byte12
    val short13: Short = Consts.short13
    val charC: Char = Consts.charC
    val long14: Long = Consts.long14
    val float15: Float = Consts.float15
    val double16: Double = Consts.double16

    val uint21: UInt = Consts.uint21
    val ubyte22: UByte = Consts.ubyte22
    val ushort23: UShort = Consts.ushort23
    val ulong24: ULong = Consts.ulong24

    val string: String = Consts.string

    object Consts {
        const val booleanFalse: Boolean = false
        const val booleanTrue: Boolean = true

        const val int11: Int = 11
        const val byte12: Byte = 12
        const val short13: Short = 13
        const val charC: Char = 'C'
        const val long14: Long = 14
        const val float15: Float = 1.5f
        const val double16: Double = 1.6

        const val uint21: UInt = 21u
        const val ubyte22: UByte = 22u
        const val ushort23: UShort = 23u
        const val ulong24: ULong = 24u

        const val string: String = "Hello, world!"
    }
}
