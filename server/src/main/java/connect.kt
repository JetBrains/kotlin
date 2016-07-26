class ConnectionRequest private constructor (ip: String = "", port: Int = 0) {
  var ip : String
    private set

  var port : Int
    private set


  init {
    this.ip = ip
    this.port = port
  }

  fun writeTo (output: CodedOutputStream) {
    if (ip != "") {
      output.writeString (1, ip)
    }
    if (port != 0) {
      output.writeInt32 (2, port)
    }
  }

  class BuilderConnectionRequest constructor (ip: String = "", port: Int = 0) {
    var ip : String
      private set
    fun setIp(value: String): ConnectionRequest.BuilderConnectionRequest {
      ip = value
      return this
    }

    var port : Int
      private set
    fun setPort(value: Int): ConnectionRequest.BuilderConnectionRequest {
      port = value
      return this
    }


    init {
      this.ip = ip
      this.port = port
    }

    fun writeTo (output: CodedOutputStream) {
      if (ip != "") {
        output.writeString (1, ip)
      }
      if (port != 0) {
        output.writeInt32 (2, port)
      }
    }

    fun build(): ConnectionRequest {
      return ConnectionRequest(ip, port)
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
          ip = input.readStringNoTag()
        }
        2 -> {
          if (wireType != WireType.VARINT) {
            throw InvalidProtocolBufferException("Error: Field number 2 has wire type WireType.VARINT but read ${wireType.toString()}")}
          port = input.readInt32NoTag()
        }
      }
      return true}
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): ConnectionRequest.BuilderConnectionRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): ConnectionRequest.BuilderConnectionRequest {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (ip != "") {
        size += WireFormat.getStringSize(1, ip)
      }
      if (port != 0) {
        size += WireFormat.getInt32Size(2, port)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }
    fun getSizeNoTag(): Int {
      var size = 0
      if (ip != "") {
        size += WireFormat.getStringSize(1, ip)
      }
      if (port != 0) {
        size += WireFormat.getInt32Size(2, port)
      }
      return size
    }
  }


  fun mergeWith (other: ConnectionRequest) {
    ip = other.ip
    port = other.port
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = ConnectionRequest.BuilderConnectionRequest()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = ConnectionRequest.BuilderConnectionRequest()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (ip != "") {
      size += WireFormat.getStringSize(1, ip)
    }
    if (port != 0) {
      size += WireFormat.getInt32Size(2, port)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }
  fun getSizeNoTag(): Int {
    var size = 0
    if (ip != "") {
      size += WireFormat.getStringSize(1, ip)
    }
    if (port != 0) {
      size += WireFormat.getInt32Size(2, port)
    }
    return size
  }
}


class ConnectionResponse private constructor (uid: Int = 0, code: Int = 0, errorMsg: String = "") {
  var uid : Int
    private set

  var code : Int
    private set

  var errorMsg : String
    private set


  init {
    this.uid = uid
    this.code = code
    this.errorMsg = errorMsg
  }

  fun writeTo (output: CodedOutputStream) {
    if (uid != 0) {
      output.writeInt32 (1, uid)
    }
    if (code != 0) {
      output.writeInt32 (2, code)
    }
    if (errorMsg != "") {
      output.writeString (3, errorMsg)
    }
  }

  class BuilderConnectionResponse constructor (uid: Int = 0, code: Int = 0, errorMsg: String = "") {
    var uid : Int
      private set
    fun setUid(value: Int): ConnectionResponse.BuilderConnectionResponse {
      uid = value
      return this
    }

    var code : Int
      private set
    fun setCode(value: Int): ConnectionResponse.BuilderConnectionResponse {
      code = value
      return this
    }

    var errorMsg : String
      private set
    fun setErrorMsg(value: String): ConnectionResponse.BuilderConnectionResponse {
      errorMsg = value
      return this
    }


    init {
      this.uid = uid
      this.code = code
      this.errorMsg = errorMsg
    }

    fun writeTo (output: CodedOutputStream) {
      if (uid != 0) {
        output.writeInt32 (1, uid)
      }
      if (code != 0) {
        output.writeInt32 (2, code)
      }
      if (errorMsg != "") {
        output.writeString (3, errorMsg)
      }
    }

    fun build(): ConnectionResponse {
      return ConnectionResponse(uid, code, errorMsg)
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
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): ConnectionResponse.BuilderConnectionResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): ConnectionResponse.BuilderConnectionResponse {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (uid != 0) {
        size += WireFormat.getInt32Size(1, uid)
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
      if (uid != 0) {
        size += WireFormat.getInt32Size(1, uid)
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


  fun mergeWith (other: ConnectionResponse) {
    uid = other.uid
    code = other.code
    errorMsg = other.errorMsg
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = ConnectionResponse.BuilderConnectionResponse()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = ConnectionResponse.BuilderConnectionResponse()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (uid != 0) {
      size += WireFormat.getInt32Size(1, uid)
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
    if (uid != 0) {
      size += WireFormat.getInt32Size(1, uid)
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


