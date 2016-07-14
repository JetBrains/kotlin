class Person private constructor (name: kotlin.String? = "", id: Int? = 0, email: kotlin.String? = "", phones: MutableList <PhoneNumber>  = mutableListOf(), someBytes: ByteArray? = ByteArray(0)) {
  var name : kotlin.String?
    private set

  var id : Int?
    private set

  var email : kotlin.String?
    private set

  var phones : MutableList <PhoneNumber> 
    private set

  var someBytes : ByteArray?
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
  class PhoneNumber private constructor (number: kotlin.String? = "", type: PhoneType? = PhoneType.fromIntToPhoneType(0)) {
    var number : kotlin.String?
      private set

    var type : PhoneType?
      private set


    init {
      this.number = number
      this.type = type
    }

    fun writeTo (output: CodedOutputStream): Unit {
      writeToNoTag(output)
    }

    fun writeToNoTag (output: CodedOutputStream): Unit {
      output.writeString (1, number)
      output.writeEnum (2, type?.ord)
    }

    class BuilderPhoneNumber constructor (number: kotlin.String? = "", type: PhoneType? = PhoneType.fromIntToPhoneType(0)) {
      var number : kotlin.String?
        private set
      fun setNumber(value: kotlin.String?): BuilderPhoneNumber {
        number = value
        return this
      }

      var type : PhoneType?
        private set
      fun setType(value: PhoneType?): BuilderPhoneNumber {
        type = value
        return this
      }


      init {
        this.number = number
        this.type = type
      }

      fun readFrom (input: CodedInputStream): BuilderPhoneNumber {
        return readFromNoTag(input)
      }

      fun readFromNoTag (input: CodedInputStream): BuilderPhoneNumber {
        number = input.readString(1)
        type = PhoneType.fromIntToPhoneType(input.readEnum(2))
        return this
}

      fun build(): PhoneNumber {
        return PhoneNumber(number, type)
      }
    }

    fun mergeFrom (input: CodedInputStream) {
      number = input.readString(1)
      type = PhoneType.fromIntToPhoneType(input.readEnum(2))
    }
  }


  fun writeTo (output: CodedOutputStream): Unit {
    writeToNoTag(output)
  }

  fun writeToNoTag (output: CodedOutputStream): Unit {
    output.writeString (1, name)
    output.writeInt32 (2, id)
    output.writeString (3, email)
    if (phones.size > 0) {
      output.writeTag(4, WireType.LENGTH_DELIMITED)
      output.writeInt32NoTag(phones.size)
      output.writeInt32NoTag(phones.size)
      for (item in phones) {
        output.writeTag(4, WireType.LENGTH_DELIMITED)
        item.writeToNoTag(output)
      }
    }
    output.writeBytes (5, someBytes)
  }

  class BuilderPerson constructor (name: kotlin.String? = "", id: Int? = 0, email: kotlin.String? = "", phones: MutableList <PhoneNumber>  = mutableListOf(), someBytes: ByteArray? = ByteArray(0)) {
    var name : kotlin.String?
      private set
    fun setName(value: kotlin.String?): BuilderPerson {
      name = value
      return this
    }

    var id : Int?
      private set
    fun setId(value: Int?): BuilderPerson {
      id = value
      return this
    }

    var email : kotlin.String?
      private set
    fun setEmail(value: kotlin.String?): BuilderPerson {
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

    var someBytes : ByteArray?
      private set
    fun setSomeBytes(value: ByteArray?): BuilderPerson {
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
      return readFromNoTag(input)
    }

    fun readFromNoTag (input: CodedInputStream): BuilderPerson {
      name = input.readString(1)
      id = input.readInt32(2)
      email = input.readString(3)
      if (phones.size > 0) {
        val tag = input.readTag(4, WireType.LENGTH_DELIMITED)
        val listSize = input.readInt32NoTag()
        for (i in 1..listSize) {
          phones[i - 1].mergeFrom(input)
        }
      }
      someBytes = input.readBytes(5)
      return this
}

    fun build(): Person {
      return Person(name, id, email, phones, someBytes)
    }
  }

  fun mergeFrom (input: CodedInputStream) {
    name = input.readString(1)
    id = input.readInt32(2)
    email = input.readString(3)
    if (phones.size > 0) {
      val tag = input.readTag(4, WireType.LENGTH_DELIMITED)
      val listSize = input.readInt32NoTag()
      for (i in 1..listSize) {
        phones[i - 1].mergeFrom(input)
      }
    }
    someBytes = input.readBytes(5)
  }
}


class AddressBook private constructor (people: MutableList <Person>  = mutableListOf()) {
  var people : MutableList <Person> 
    private set


  init {
    this.people = people
  }

  fun writeTo (output: CodedOutputStream): Unit {
    writeToNoTag(output)
  }

  fun writeToNoTag (output: CodedOutputStream): Unit {
    if (people.size > 0) {
      output.writeTag(1, WireType.LENGTH_DELIMITED)
      output.writeInt32NoTag(people.size)
      output.writeInt32NoTag(people.size)
      for (item in people) {
        output.writeTag(1, WireType.LENGTH_DELIMITED)
        item.writeToNoTag(output)
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
      return readFromNoTag(input)
    }

    fun readFromNoTag (input: CodedInputStream): BuilderAddressBook {
      if (people.size > 0) {
        val tag = input.readTag(1, WireType.LENGTH_DELIMITED)
        val listSize = input.readInt32NoTag()
        for (i in 1..listSize) {
          people[i - 1].mergeFrom(input)
        }
      }
      return this
}

    fun build(): AddressBook {
      return AddressBook(people)
    }
  }

  fun mergeFrom (input: CodedInputStream) {
    if (people.size > 0) {
      val tag = input.readTag(1, WireType.LENGTH_DELIMITED)
      val listSize = input.readInt32NoTag()
      for (i in 1..listSize) {
        people[i - 1].mergeFrom(input)
      }
    }
  }
}


