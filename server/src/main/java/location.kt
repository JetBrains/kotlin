class LocationResponse private constructor (locationResponseData: LocationResponse.LocationData = LocationResponse.LocationData.BuilderLocationData().build(), code: Int = 0, errorMsg: String = "") {
  var locationResponseData : LocationResponse.LocationData
    private set

  var code : Int
    private set

  var errorMsg : String
    private set


  init {
    this.locationResponseData = locationResponseData
    this.code = code
    this.errorMsg = errorMsg
  }
  class LocationData private constructor (x: Double = 0.0, y: Double = 0.0, angle: Double = 0.0) {
    var x : Double
      private set

    var y : Double
      private set

    var angle : Double
      private set


    init {
      this.x = x
      this.y = y
      this.angle = angle
    }

    fun writeTo (output: CodedOutputStream) {
      if (x != 0.0) {
        output.writeDouble (1, x)
      }
      if (y != 0.0) {
        output.writeDouble (2, y)
      }
      if (angle != 0.0) {
        output.writeDouble (3, angle)
      }
    }

    class BuilderLocationData constructor (x: Double = 0.0, y: Double = 0.0, angle: Double = 0.0) {
      var x : Double
        private set
      fun setX(value: Double): LocationResponse.LocationData.BuilderLocationData {
        x = value
        return this
      }

      var y : Double
        private set
      fun setY(value: Double): LocationResponse.LocationData.BuilderLocationData {
        y = value
        return this
      }

      var angle : Double
        private set
      fun setAngle(value: Double): LocationResponse.LocationData.BuilderLocationData {
        angle = value
        return this
      }


      init {
        this.x = x
        this.y = y
        this.angle = angle
      }

      fun writeTo (output: CodedOutputStream) {
        if (x != 0.0) {
          output.writeDouble (1, x)
        }
        if (y != 0.0) {
          output.writeDouble (2, y)
        }
        if (angle != 0.0) {
          output.writeDouble (3, angle)
        }
      }

      fun build(): LocationResponse.LocationData {
        return LocationResponse.LocationData(x, y, angle)
      }

      fun parseFieldFrom(input: CodedInputStream): Boolean {
        if (input.isAtEnd()) { return false }
        val tag = input.readInt32NoTag()
        if (tag == 0) { return false } 
        val fieldNumber = WireFormat.getTagFieldNumber(tag)
        val wireType = WireFormat.getTagWireType(tag)
        when(fieldNumber) {
          1 -> {
            if (wireType != WireType.FIX_64) {
              throw InvalidProtocolBufferException("Error: Field number 1 has wire type WireType.FIX_64 but read ${wireType.toString()}")}
            x = input.readDoubleNoTag()
          }
          2 -> {
            if (wireType != WireType.FIX_64) {
              throw InvalidProtocolBufferException("Error: Field number 2 has wire type WireType.FIX_64 but read ${wireType.toString()}")}
            y = input.readDoubleNoTag()
          }
          3 -> {
            if (wireType != WireType.FIX_64) {
              throw InvalidProtocolBufferException("Error: Field number 3 has wire type WireType.FIX_64 but read ${wireType.toString()}")}
            angle = input.readDoubleNoTag()
          }
        }
        return true}
      fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): LocationResponse.LocationData.BuilderLocationData {
        while(getSizeNoTag() < expectedSize) {
          parseFieldFrom(input)
        }
        if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
        return this
      }
      fun parseFrom(input: CodedInputStream): LocationResponse.LocationData.BuilderLocationData {
        while(parseFieldFrom(input)) {}
        return this
      }
      fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (x != 0.0) {
          size += WireFormat.getDoubleSize(1, x)
        }
        if (y != 0.0) {
          size += WireFormat.getDoubleSize(2, y)
        }
        if (angle != 0.0) {
          size += WireFormat.getDoubleSize(3, angle)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
      }
      fun getSizeNoTag(): Int {
        var size = 0
        if (x != 0.0) {
          size += WireFormat.getDoubleSize(1, x)
        }
        if (y != 0.0) {
          size += WireFormat.getDoubleSize(2, y)
        }
        if (angle != 0.0) {
          size += WireFormat.getDoubleSize(3, angle)
        }
        return size
      }
    }


    fun mergeWith (other: LocationResponse.LocationData) {
      x = other.x
      y = other.y
      angle = other.angle
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
      val builder = LocationResponse.LocationData.BuilderLocationData()
      mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

    fun mergeFrom (input: CodedInputStream) {
      val builder = LocationResponse.LocationData.BuilderLocationData()
      mergeWith(builder.parseFrom(input).build())}
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (x != 0.0) {
        size += WireFormat.getDoubleSize(1, x)
      }
      if (y != 0.0) {
        size += WireFormat.getDoubleSize(2, y)
      }
      if (angle != 0.0) {
        size += WireFormat.getDoubleSize(3, angle)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }
    fun getSizeNoTag(): Int {
      var size = 0
      if (x != 0.0) {
        size += WireFormat.getDoubleSize(1, x)
      }
      if (y != 0.0) {
        size += WireFormat.getDoubleSize(2, y)
      }
      if (angle != 0.0) {
        size += WireFormat.getDoubleSize(3, angle)
      }
      return size
    }
  }


  fun writeTo (output: CodedOutputStream) {
    if (locationResponseData != LocationResponse.LocationData.BuilderLocationData().build()) {
      output.writeTag(1, WireType.LENGTH_DELIMITED)
      output.writeInt32NoTag(locationResponseData.getSizeNoTag())
      locationResponseData.writeTo(output)
    }
    if (code != 0) {
      output.writeInt32 (2, code)
    }
    if (errorMsg != "") {
      output.writeString (3, errorMsg)
    }
  }

  class BuilderLocationResponse constructor (locationResponseData: LocationResponse.LocationData = LocationResponse.LocationData.BuilderLocationData().build(), code: Int = 0, errorMsg: String = "") {
    var locationResponseData : LocationResponse.LocationData
      private set
    fun setLocationResponseData(value: LocationResponse.LocationData): LocationResponse.BuilderLocationResponse {
      locationResponseData = value
      return this
    }

    var code : Int
      private set
    fun setCode(value: Int): LocationResponse.BuilderLocationResponse {
      code = value
      return this
    }

    var errorMsg : String
      private set
    fun setErrorMsg(value: String): LocationResponse.BuilderLocationResponse {
      errorMsg = value
      return this
    }


    init {
      this.locationResponseData = locationResponseData
      this.code = code
      this.errorMsg = errorMsg
    }

    fun writeTo (output: CodedOutputStream) {
      if (locationResponseData != LocationResponse.LocationData.BuilderLocationData().build()) {
        output.writeTag(1, WireType.LENGTH_DELIMITED)
        output.writeInt32NoTag(locationResponseData.getSizeNoTag())
        locationResponseData.writeTo(output)
      }
      if (code != 0) {
        output.writeInt32 (2, code)
      }
      if (errorMsg != "") {
        output.writeString (3, errorMsg)
      }
    }

    fun build(): LocationResponse {
      return LocationResponse(locationResponseData, code, errorMsg)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> {
          if (wireType != WireType.LENGTH_DELIMITED) {
            throw InvalidProtocolBufferException("Error: Field number 1 has wire type WireType.LENGTH_DELIMITED but read ${wireType.toString()}")}
          run {
            val expectedSize = input.readInt32NoTag()
            locationResponseData.mergeFromWithSize(input, expectedSize)
            if (expectedSize != locationResponseData.getSizeNoTag()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${locationResponseData.getSizeNoTag()}") }
          }
        }
        2 -> {
          if (wireType != WireType.VARINT) {
            throw InvalidProtocolBufferException("Error: Field number 2 has wire type WireType.VARINT but read ${wireType.toString()}")}
          code = input.readInt32NoTag()
        }
        3 -> {
          if (wireType != WireType.LENGTH_DELIMITED) {
            throw InvalidProtocolBufferException("Error: Field number 3 has wire type WireType.LENGTH_DELIMITED but read ${wireType.toString()}")}
          errorMsg = input.readStringNoTag()
        }
      }
      return true}
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): LocationResponse.BuilderLocationResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): LocationResponse.BuilderLocationResponse {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (locationResponseData != LocationResponse.LocationData.BuilderLocationData().build()) {
        size += locationResponseData.getSize(1)
      }
      if (code != 0) {
        size += WireFormat.getInt32Size(2, code)
      }
      if (errorMsg != "") {
        size += WireFormat.getStringSize(3, errorMsg)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }
    fun getSizeNoTag(): Int {
      var size = 0
      if (locationResponseData != LocationResponse.LocationData.BuilderLocationData().build()) {
        size += locationResponseData.getSize(1)
      }
      if (code != 0) {
        size += WireFormat.getInt32Size(2, code)
      }
      if (errorMsg != "") {
        size += WireFormat.getStringSize(3, errorMsg)
      }
      return size
    }
  }


  fun mergeWith (other: LocationResponse) {
    locationResponseData = other.locationResponseData
    code = other.code
    errorMsg = other.errorMsg
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = LocationResponse.BuilderLocationResponse()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = LocationResponse.BuilderLocationResponse()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (locationResponseData != LocationResponse.LocationData.BuilderLocationData().build()) {
      size += locationResponseData.getSize(1)
    }
    if (code != 0) {
      size += WireFormat.getInt32Size(2, code)
    }
    if (errorMsg != "") {
      size += WireFormat.getStringSize(3, errorMsg)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }
  fun getSizeNoTag(): Int {
    var size = 0
    if (locationResponseData != LocationResponse.LocationData.BuilderLocationData().build()) {
      size += locationResponseData.getSize(1)
    }
    if (code != 0) {
      size += WireFormat.getInt32Size(2, code)
    }
    if (errorMsg != "") {
      size += WireFormat.getStringSize(3, errorMsg)
    }
    return size
  }
}


