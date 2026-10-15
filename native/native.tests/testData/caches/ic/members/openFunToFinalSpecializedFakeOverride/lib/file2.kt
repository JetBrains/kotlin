package test

fun viaByte(impl: ByteImpl) = impl.insert(Builder("b"), 1, 7.toByte())

fun viaLong(impl: LongImpl) = impl.insert(Builder("l"), 2, 9L)

fun viaRef(impl: RefImpl) = impl.insert(Builder("r"), 3, "s")

fun viaMid(impl: Mid<Byte, Builder>) = impl.insert(Builder("m"), 4, 5.toByte())
