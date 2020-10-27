package sample.androidnative

import kotlinx.cinterop.*
import sample.androidnative.bmpformat.BMPHeader

val BMPHeader.data
    get() = (ptr.reinterpret<ByteVar>() + sizeOf<BMPHeader>()) as CArrayPointer<ByteVar>
