class Level1 private constructor (field1: Level2 = Level1.Level2.BuilderLevel2().build()) {
  var field1 : Level2
    private set


  init {
    this.field1 = field1
  }
  class Level2 private constructor (field2: Level3 = Level1.Level2.Level3.BuilderLevel3().build()) {
    var field2 : Level3
      private set


    init {
      this.field2 = field2
    }
    class Level3 private constructor (field3: Level4 = Level1.Level2.Level3.Level4.BuilderLevel4().build()) {
      var field3 : Level4
        private set


      init {
        this.field3 = field3
      }
      class Level4 private constructor (field4: kotlin.String = "") {
        var field4 : kotlin.String
          private set


        init {
          this.field4 = field4
        }

        fun writeTo (output: CodedOutputStream): Unit {
          output.writeString (4, field4)
        }

        class BuilderLevel4 constructor (field4: kotlin.String = "") {
          var field4 : kotlin.String
            private set
          fun setField4(value: kotlin.String): Level1.Level2.Level3.Level4.BuilderLevel4 {
            field4 = value
            return this
          }


          init {
            this.field4 = field4
          }

          fun readFrom (input: CodedInputStream): Level1.Level2.Level3.Level4.BuilderLevel4 {
            field4 = input.readString(4)
            return this
}

          fun build(): Level4 {
            return Level4(field4)
          }

          fun parseFieldFrom(input: CodedInputStream): Boolean {
            if (input.isAtEnd()) { return false }
            val tag = input.readInt32NoTag()
            if (tag == 0) { return false } 
            val fieldNumber = WireFormat.getTagFieldNumber(tag)
            val wireType = WireFormat.getTagWireType(tag)
            when(fieldNumber) {
              4 -> field4 = input.readStringNoTag()
            }
            return true}
          fun parseFrom(input: CodedInputStream): Level1.Level2.Level3.Level4.BuilderLevel4 {
            while(parseFieldFrom(input)) {}
            return this
          }
          fun getSize(): Int {
            var size = 0
            size += WireFormat.getStringSize(4, field4)
            return size
          }
        }


        fun mergeWith (other: Level4) {
          field4 = other.field4
        }

        fun mergeFrom (input: CodedInputStream) {
          val builder = Level1.Level2.Level3.Level4.BuilderLevel4()
          mergeWith(builder.parseFrom(input).build())}
        fun getSize(): Int {
          var size = 0
          size += WireFormat.getStringSize(4, field4)
          return size
        }
      }


      fun writeTo (output: CodedOutputStream): Unit {
        output.writeTag(3, WireType.LENGTH_DELIMITED)
        output.writeInt32NoTag(field3.getSize())
        field3.writeTo(output)
      }

      class BuilderLevel3 constructor (field3: Level4 = Level1.Level2.Level3.Level4.BuilderLevel4().build()) {
        var field3 : Level4
          private set
        fun setField3(value: Level4): Level1.Level2.Level3.BuilderLevel3 {
          field3 = value
          return this
        }


        init {
          this.field3 = field3
        }

        fun readFrom (input: CodedInputStream): Level1.Level2.Level3.BuilderLevel3 {
          input.readTag(3, WireType.LENGTH_DELIMITED)
          val expectedSize = input.readInt32NoTag()
          field3.mergeFrom(input)
          if (expectedSize != field3.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${field3.getSize()}") }
          return this
}

        fun build(): Level3 {
          return Level3(field3)
        }

        fun parseFieldFrom(input: CodedInputStream): Boolean {
          if (input.isAtEnd()) { return false }
          val tag = input.readInt32NoTag()
          if (tag == 0) { return false } 
          val fieldNumber = WireFormat.getTagFieldNumber(tag)
          val wireType = WireFormat.getTagWireType(tag)
          when(fieldNumber) {
            3 -> {
              input.readTag(3, WireType.LENGTH_DELIMITED)
              val expectedSize = input.readInt32NoTag()
              field3.mergeFrom(input)
              if (expectedSize != field3.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${field3.getSize()}") }
            }
          }
          return true}
        fun parseFrom(input: CodedInputStream): Level1.Level2.Level3.BuilderLevel3 {
          while(parseFieldFrom(input)) {}
          return this
        }
        fun getSize(): Int {
          var size = 0
          size += field3.getSize() + WireFormat.getTagSize(3, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(field3.getSize())
          return size
        }
      }


      fun mergeWith (other: Level3) {
        field3 = other.field3
      }

      fun mergeFrom (input: CodedInputStream) {
        val builder = Level1.Level2.Level3.BuilderLevel3()
        mergeWith(builder.parseFrom(input).build())}
      fun getSize(): Int {
        var size = 0
        size += field3.getSize() + WireFormat.getTagSize(3, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(field3.getSize())
        return size
      }
    }


    fun writeTo (output: CodedOutputStream): Unit {
      output.writeTag(2, WireType.LENGTH_DELIMITED)
      output.writeInt32NoTag(field2.getSize())
      field2.writeTo(output)
    }

    class BuilderLevel2 constructor (field2: Level3 = Level1.Level2.Level3.BuilderLevel3().build()) {
      var field2 : Level3
        private set
      fun setField2(value: Level3): Level1.Level2.BuilderLevel2 {
        field2 = value
        return this
      }


      init {
        this.field2 = field2
      }

      fun readFrom (input: CodedInputStream): Level1.Level2.BuilderLevel2 {
        input.readTag(2, WireType.LENGTH_DELIMITED)
        val expectedSize = input.readInt32NoTag()
        field2.mergeFrom(input)
        if (expectedSize != field2.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${field2.getSize()}") }
        return this
}

      fun build(): Level2 {
        return Level2(field2)
      }

      fun parseFieldFrom(input: CodedInputStream): Boolean {
        if (input.isAtEnd()) { return false }
        val tag = input.readInt32NoTag()
        if (tag == 0) { return false } 
        val fieldNumber = WireFormat.getTagFieldNumber(tag)
        val wireType = WireFormat.getTagWireType(tag)
        when(fieldNumber) {
          2 -> {
            input.readTag(2, WireType.LENGTH_DELIMITED)
            val expectedSize = input.readInt32NoTag()
            field2.mergeFrom(input)
            if (expectedSize != field2.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${field2.getSize()}") }
          }
        }
        return true}
      fun parseFrom(input: CodedInputStream): Level1.Level2.BuilderLevel2 {
        while(parseFieldFrom(input)) {}
        return this
      }
      fun getSize(): Int {
        var size = 0
        size += field2.getSize() + WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(field2.getSize())
        return size
      }
    }


    fun mergeWith (other: Level2) {
      field2 = other.field2
    }

    fun mergeFrom (input: CodedInputStream) {
      val builder = Level1.Level2.BuilderLevel2()
      mergeWith(builder.parseFrom(input).build())}
    fun getSize(): Int {
      var size = 0
      size += field2.getSize() + WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(field2.getSize())
      return size
    }
  }


  fun writeTo (output: CodedOutputStream): Unit {
    output.writeTag(1, WireType.LENGTH_DELIMITED)
    output.writeInt32NoTag(field1.getSize())
    field1.writeTo(output)
  }

  class BuilderLevel1 constructor (field1: Level2 = Level1.Level2.BuilderLevel2().build()) {
    var field1 : Level2
      private set
    fun setField1(value: Level2): Level1.BuilderLevel1 {
      field1 = value
      return this
    }


    init {
      this.field1 = field1
    }

    fun readFrom (input: CodedInputStream): Level1.BuilderLevel1 {
      input.readTag(1, WireType.LENGTH_DELIMITED)
      val expectedSize = input.readInt32NoTag()
      field1.mergeFrom(input)
      if (expectedSize != field1.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${field1.getSize()}") }
      return this
}

    fun build(): Level1 {
      return Level1(field1)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> {
          input.readTag(1, WireType.LENGTH_DELIMITED)
          val expectedSize = input.readInt32NoTag()
          field1.mergeFrom(input)
          if (expectedSize != field1.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${field1.getSize()}") }
        }
      }
      return true}
    fun parseFrom(input: CodedInputStream): Level1.BuilderLevel1 {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(): Int {
      var size = 0
      size += field1.getSize() + WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(field1.getSize())
      return size
    }
  }


  fun mergeWith (other: Level1) {
    field1 = other.field1
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = Level1.BuilderLevel1()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(): Int {
    var size = 0
    size += field1.getSize() + WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(field1.getSize())
    return size
  }
}


