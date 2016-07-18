class Person private constructor (name: kotlin.String = "", id: Int = 0, email: kotlin.String = "", phones: MutableList <PhoneNumber>  = mutableListOf(), someBytes: ByteArray = ByteArray(0)) {
  var name : kotlin.String
    private set

  var id : Int
    private set

  var email : kotlin.String
    private set

  var phones : MutableList <PhoneNumber> 
    private set

  var someBytes : ByteArray
    private set


  init {
    this.name = name
    this.id = id
    this.email = email
    this.phones = phones
    this.someBytes = someBytes
  }
  enum class PhoneType(val ord: Int) {
    MOBILE (0),
    HOME (1),
    WORK (2);

    companion object {
      fun fromIntToPhoneType (ord: Int): PhoneType {
        return when (ord) {
          0 -> PhoneType.MOBILE
          1 -> PhoneType.HOME
          2 -> PhoneType.WORK
          else -> throw InvalidProtocolBufferException("Error: got unexpected int ${ord} while parsing PhoneType ");
        }
      }
    }
  }
  class PhoneNumber private constructor (number: kotlin.String = "", type: PhoneType = PhoneType.fromIntToPhoneType(0)) {
    var number : kotlin.String
      private set

    var type : PhoneType
      private set


    init {
      this.number = number
      this.type = type
    }

    fun writeTo (output: CodedOutputStream): Unit {
      output.writeString (1, number)
      output.writeEnum (2, type.ord)
    }

    class BuilderPhoneNumber constructor (number: kotlin.String = "", type: PhoneType = PhoneType.fromIntToPhoneType(0)) {
      var number : kotlin.String
        private set
      fun setNumber(value: kotlin.String): BuilderPhoneNumber {
        number = value
        return this
      }

      var type : PhoneType
        private set
      fun setType(value: PhoneType): BuilderPhoneNumber {
        type = value
        return this
      }


      init {
        this.number = number
        this.type = type
      }

      fun readFrom (input: CodedInputStream): BuilderPhoneNumber {
        number = input.readString(1)
        type = PhoneType.fromIntToPhoneType(input.readEnum(2))
        return this
}

      fun build(): PhoneNumber {
        return PhoneNumber(number, type)
      }

      fun parseFieldFrom(input: CodedInputStream): Boolean {
        if (input.isAtEnd()) { return false }
        val tag = input.readInt32NoTag()
        if (tag == 0) { return false } 
        val fieldNumber = WireFormat.getTagFieldNumber(tag)
        val wireType = WireFormat.getTagWireType(tag)
        when(fieldNumber) {
          1 -> number = input.readStringNoTag()
          2 -> type = PhoneType.fromIntToPhoneType(input.readEnumNoTag())
        }
        return true}
      fun parseFrom(input: CodedInputStream): BuilderPhoneNumber {
        while(parseFieldFrom(input)) {}
        return this
      }
      fun getSize(): Int {
        var size = 0
        size += WireFormat.getStringSize(1, number)
        size += WireFormat.getEnumSize(2, type.ord)
        return size
      }
    }


    fun mergeWith (other: PhoneNumber) {
      number = other.number
      type = other.type
    }

    fun mergeFrom (input: CodedInputStream) {
      val builder = BuilderPhoneNumber()
      mergeWith(builder.parseFrom(input).build())}
    fun getSize(): Int {
      var size = 0
      size += WireFormat.getStringSize(1, number)
      size += WireFormat.getEnumSize(2, type.ord)
      return size
    }
  }


  fun writeTo (output: CodedOutputStream): Unit {
    output.writeString (1, name)
    output.writeInt32 (2, id)
    output.writeString (3, email)
    if (phones.size > 0) {
      output.writeTag(4, WireType.LENGTH_DELIMITED)
      var arrayByteSize = 0
      run {
        var arraySize = 0
        for (item in phones) {
          arraySize += item.getSize() + WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(item.getSize())
        }
        arrayByteSize += arraySize
      }
      output.writeInt32NoTag(arrayByteSize)
      for (item in phones) {
        output.writeTag(4, WireType.LENGTH_DELIMITED)
        output.writeInt32NoTag(item.getSize())
        item.writeTo(output)
      }
    }
    output.writeBytes (5, someBytes)
  }

  class BuilderPerson constructor (name: kotlin.String = "", id: Int = 0, email: kotlin.String = "", phones: MutableList <PhoneNumber>  = mutableListOf(), someBytes: ByteArray = ByteArray(0)) {
    var name : kotlin.String
      private set
    fun setName(value: kotlin.String): BuilderPerson {
      name = value
      return this
    }

    var id : Int
      private set
    fun setId(value: Int): BuilderPerson {
      id = value
      return this
    }

    var email : kotlin.String
      private set
    fun setEmail(value: kotlin.String): BuilderPerson {
      email = value
      return this
    }

    var phones : MutableList <PhoneNumber> 
      private set
    fun setPhones(value: MutableList <PhoneNumber> ): BuilderPerson {
      phones = value
      return this
    }
    fun setPhoneNumber(index: Int, value: PhoneNumber): BuilderPerson {
      phones[index] = value
      return this
    }
    fun addPhoneNumber(value: PhoneNumber): BuilderPerson {
      phones.add(value)
      return this
    }
    fun addAllPhoneNumber(value: Iterable<PhoneNumber>): BuilderPerson {
      for (item in value) {
        phones.add(item)
      }
      return this
    }

    var someBytes : ByteArray
      private set
    fun setSomeBytes(value: ByteArray): BuilderPerson {
      someBytes = value
      return this
    }


    init {
      this.name = name
      this.id = id
      this.email = email
      this.phones = phones
      this.someBytes = someBytes
    }

    fun readFrom (input: CodedInputStream): BuilderPerson {
      name = input.readString(1)
      id = input.readInt32(2)
      email = input.readString(3)
      val tag = input.readTag(4, WireType.LENGTH_DELIMITED)
      val expectedSize = input.readInt32NoTag()
      var readSize = 0
      while(readSize != expectedSize) {
        val tmp: PhoneNumber.BuilderPhoneNumber = PhoneNumber.BuilderPhoneNumber()
        input.readTag(4, WireType.LENGTH_DELIMITED)
        val expectedSize = input.readInt32NoTag()
        tmp.readFrom(input)
        if (expectedSize != tmp.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${tmp.getSize()}") }
        readSize += tmp.getSize() + WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(tmp.getSize())
        phones.add(tmp.build())
      }
      someBytes = input.readBytes(5)
      return this
}

    fun build(): Person {
      return Person(name, id, email, phones, someBytes)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> name = input.readStringNoTag()
        2 -> id = input.readInt32NoTag()
        3 -> email = input.readStringNoTag()
        4 -> {
          val expectedSize = input.readInt32NoTag()
          var readSize = 0
          while(readSize != expectedSize) {
            val tmp: PhoneNumber.BuilderPhoneNumber = PhoneNumber.BuilderPhoneNumber()
            input.readTag(4, WireType.LENGTH_DELIMITED)
            val expectedSize = input.readInt32NoTag()
            tmp.readFrom(input)
            if (expectedSize != tmp.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${tmp.getSize()}") }
            readSize += tmp.getSize() + WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(tmp.getSize())
            phones.add(tmp.build())
          }
        }
        5 -> someBytes = input.readBytesNoTag()
      }
      return true}
    fun parseFrom(input: CodedInputStream): BuilderPerson {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(): Int {
      var size = 0
      size += WireFormat.getStringSize(1, name)
      size += WireFormat.getInt32Size(2, id)
      size += WireFormat.getStringSize(3, email)
      run {
        var arraySize = 0
        for (item in phones) {
          arraySize += item.getSize() + WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(item.getSize())
        }
        size += arraySize + WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(arraySize)
      }
      size += WireFormat.getBytesSize(5, someBytes)
      return size
    }
  }


  fun mergeWith (other: Person) {
    name = other.name
    id = other.id
    email = other.email
    phones.addAll(other.phones)
    someBytes.plus(other.someBytes)
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = BuilderPerson()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(): Int {
    var size = 0
    size += WireFormat.getStringSize(1, name)
    size += WireFormat.getInt32Size(2, id)
    size += WireFormat.getStringSize(3, email)
    run {
      var arraySize = 0
      for (item in phones) {
        arraySize += item.getSize() + WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(item.getSize())
      }
      size += arraySize + WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(arraySize)
    }
    size += WireFormat.getBytesSize(5, someBytes)
    return size
  }
}


class AddressBook private constructor (people: MutableList <Person>  = mutableListOf()) {
  var people : MutableList <Person> 
    private set


  init {
    this.people = people
  }

  fun writeTo (output: CodedOutputStream): Unit {
    if (people.size > 0) {
      output.writeTag(1, WireType.LENGTH_DELIMITED)
      var arrayByteSize = 0
      run {
        var arraySize = 0
        for (item in people) {
          arraySize += item.getSize() + WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(item.getSize())
        }
        arrayByteSize += arraySize
      }
      output.writeInt32NoTag(arrayByteSize)
      for (item in people) {
        output.writeTag(1, WireType.LENGTH_DELIMITED)
        output.writeInt32NoTag(item.getSize())
        item.writeTo(output)
      }
    }
  }

  class BuilderAddressBook constructor (people: MutableList <Person>  = mutableListOf()) {
    var people : MutableList <Person> 
      private set
    fun setPeople(value: MutableList <Person> ): BuilderAddressBook {
      people = value
      return this
    }
    fun setPerson(index: Int, value: Person): BuilderAddressBook {
      people[index] = value
      return this
    }
    fun addPerson(value: Person): BuilderAddressBook {
      people.add(value)
      return this
    }
    fun addAllPerson(value: Iterable<Person>): BuilderAddressBook {
      for (item in value) {
        people.add(item)
      }
      return this
    }


    init {
      this.people = people
    }

    fun readFrom (input: CodedInputStream): BuilderAddressBook {
      val tag = input.readTag(1, WireType.LENGTH_DELIMITED)
      val expectedSize = input.readInt32NoTag()
      var readSize = 0
      while(readSize != expectedSize) {
        val tmp: Person.BuilderPerson = Person.BuilderPerson()
        input.readTag(1, WireType.LENGTH_DELIMITED)
        val expectedSize = input.readInt32NoTag()
        tmp.readFrom(input)
        if (expectedSize != tmp.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${tmp.getSize()}") }
        readSize += tmp.getSize() + WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(tmp.getSize())
        people.add(tmp.build())
      }
      return this
}

    fun build(): AddressBook {
      return AddressBook(people)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> {
          val expectedSize = input.readInt32NoTag()
          var readSize = 0
          while(readSize != expectedSize) {
            val tmp: Person.BuilderPerson = Person.BuilderPerson()
            input.readTag(1, WireType.LENGTH_DELIMITED)
            val expectedSize = input.readInt32NoTag()
            tmp.readFrom(input)
            if (expectedSize != tmp.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${tmp.getSize()}") }
            readSize += tmp.getSize() + WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(tmp.getSize())
            people.add(tmp.build())
          }
        }
      }
      return true}
    fun parseFrom(input: CodedInputStream): BuilderAddressBook {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(): Int {
      var size = 0
      run {
        var arraySize = 0
        for (item in people) {
          arraySize += item.getSize() + WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(item.getSize())
        }
        size += arraySize + WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(arraySize)
      }
      return size
    }
  }


  fun mergeWith (other: AddressBook) {
    people.addAll(other.people)
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = BuilderAddressBook()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(): Int {
    var size = 0
    run {
      var arraySize = 0
      for (item in people) {
        arraySize += item.getSize() + WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(item.getSize())
      }
      size += arraySize + WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(arraySize)
    }
    return size
  }
}


