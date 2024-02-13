// FIR_IDENTICAL

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*


class Prop1<T>(val t: T)

class Prop2<T, R>(val t: T, val r: R)

<!EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR!>@Serializer(forClass = Prop1::class)
class ExternalSerializer0_1<!>

@Serializer(forClass = Prop1::class)
class ExternalSerializer1_1(val typeSerial0: KSerializer<Any>)

@Serializer(forClass = Prop1::class)
class ExternalSerializer1_1Secondary() {
    var typeSerial0: KSerializer<Any>? = null
    constructor(typeSerial0: KSerializer<Any>) : this() {
        this.typeSerial0 = typeSerial0
    }
}

<!EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR!>@Serializer(forClass = Prop2::class)
class ExternalSerializer0_2<!>

<!EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR!>@Serializer(forClass = Prop2::class)
class ExternalSerializer1_2(val typeSerial0: KSerializer<Any>)<!>

<!EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR!>@Serializer(forClass = Prop2::class)
class ExternalSerializer1_2Secondary() {
    var typeSerial0: KSerializer<Any>? = null

    constructor(typeSerial0: KSerializer<Any>) : this() {
        this.typeSerial0 = typeSerial0
    }
}<!>

@Serializer(forClass = Prop2::class)
class ExternalSerializer2_2Secondary() {
    var typeSerial0: KSerializer<Any>? = null
    var typeSerial1: KSerializer<Any>? = null
    constructor(typeSerial0: KSerializer<Any>) : this() {
        this.typeSerial0 = typeSerial0
    }
    constructor(typeSerial0: KSerializer<Any>, typeSerial1: KSerializer<Any>) : this() {
        this.typeSerial0 = typeSerial0
        this.typeSerial1 = typeSerial1
    }
}


<!EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR!>@Serializer(forClass = Prop1Err::class)
class ExternalSerializer0_1Err<!>

// there is no error in the discrepancy between the number of constructor parameters in the serializer
// because the checks take place at the place where the serializer is declared, and if the serializer is declared correctly, there will be no error here
@Serializable(ExternalSerializer0_1Err::class)
class Prop1Err<T>(val t: T)
