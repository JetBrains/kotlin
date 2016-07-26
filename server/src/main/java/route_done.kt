class RouteDoneRequest private constructor (uid: Int = 0) {
  var uid : Int
    private set


  init {
    this.uid = uid
  }

  fun writeTo (output: CodedOutputStream) {
    if (uid != 0) {
      output.writeInt32 (1, uid)
    }
  }

  class BuilderRouteDoneRequest constructor (uid: Int = 0) {
    var uid : Int
      private set
    fun setUid(value: Int): RouteDoneRequest.BuilderRouteDoneRequest {
      uid = value
      return this
    }


    init {
      this.uid = uid
    }

    fun writeTo (output: CodedOutputStream) {
      if (uid != 0) {
        output.writeInt32 (1, uid)
      }
    }

    fun build(): RouteDoneRequest {
      return RouteDoneRequest(uid)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> {
          if (wireType != WireType.VARINT) {
            throw InvalidProtocolBufferException("Error: Field number 1 has wire type WireType.VARINT but read ${wireType.toString()}")}
          uid = input.readInt32NoTag()
        }
      }
      return true}
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): RouteDoneRequest.BuilderRouteDoneRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): RouteDoneRequest.BuilderRouteDoneRequest {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (uid != 0) {
        size += WireFormat.getInt32Size(1, uid)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }
    fun getSizeNoTag(): Int {
      var size = 0
      if (uid != 0) {
        size += WireFormat.getInt32Size(1, uid)
      }
      return size
    }
  }


  fun mergeWith (other: RouteDoneRequest) {
    uid = other.uid
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = RouteDoneRequest.BuilderRouteDoneRequest()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = RouteDoneRequest.BuilderRouteDoneRequest()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (uid != 0) {
      size += WireFormat.getInt32Size(1, uid)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }
  fun getSizeNoTag(): Int {
    var size = 0
    if (uid != 0) {
      size += WireFormat.getInt32Size(1, uid)
    }
    return size
  }
}


class RouteDoneResponse private constructor (code: Int = 0, errorMsg: String = "") {
  var code : Int
    private set

  var errorMsg : String
    private set


  init {
    this.code = code
    this.errorMsg = errorMsg
  }

  fun writeTo (output: CodedOutputStream) {
    if (code != 0) {
      output.writeInt32 (1, code)
    }
    if (errorMsg != "") {
      output.writeString (2, errorMsg)
    }
  }

  class BuilderRouteDoneResponse constructor (code: Int = 0, errorMsg: String = "") {
    var code : Int
      private set
    fun setCode(value: Int): RouteDoneResponse.BuilderRouteDoneResponse {
      code = value
      return this
    }

    var errorMsg : String
      private set
    fun setErrorMsg(value: String): RouteDoneResponse.BuilderRouteDoneResponse {
      errorMsg = value
      return this
    }


    init {
      this.code = code
      this.errorMsg = errorMsg
    }

    fun writeTo (output: CodedOutputStream) {
      if (code != 0) {
        output.writeInt32 (1, code)
      }
      if (errorMsg != "") {
        output.writeString (2, errorMsg)
      }
    }

    fun build(): RouteDoneResponse {
      return RouteDoneResponse(code, errorMsg)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> {
          if (wireType != WireType.VARINT) {
            throw InvalidProtocolBufferException("Error: Field number 1 has wire type WireType.VARINT but read ${wireType.toString()}")}
          code = input.readInt32NoTag()
        }
        2 -> {
          if (wireType != WireType.LENGTH_DELIMITED) {
            throw InvalidProtocolBufferException("Error: Field number 2 has wire type WireType.LENGTH_DELIMITED but read ${wireType.toString()}")}
          errorMsg = input.readStringNoTag()
        }
      }
      return true}
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): RouteDoneResponse.BuilderRouteDoneResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): RouteDoneResponse.BuilderRouteDoneResponse {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (code != 0) {
        size += WireFormat.getInt32Size(1, code)
      }
      if (errorMsg != "") {
        size += WireFormat.getStringSize(2, errorMsg)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }
    fun getSizeNoTag(): Int {
      var size = 0
      if (code != 0) {
        size += WireFormat.getInt32Size(1, code)
      }
      if (errorMsg != "") {
        size += WireFormat.getStringSize(2, errorMsg)
      }
      return size
    }
  }


  fun mergeWith (other: RouteDoneResponse) {
    code = other.code
    errorMsg = other.errorMsg
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = RouteDoneResponse.BuilderRouteDoneResponse()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = RouteDoneResponse.BuilderRouteDoneResponse()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (code != 0) {
      size += WireFormat.getInt32Size(1, code)
    }
    if (errorMsg != "") {
      size += WireFormat.getStringSize(2, errorMsg)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }
  fun getSizeNoTag(): Int {
    var size = 0
    if (code != 0) {
      size += WireFormat.getInt32Size(1, code)
    }
    if (errorMsg != "") {
      size += WireFormat.getStringSize(2, errorMsg)
    }
    return size
  }
}


