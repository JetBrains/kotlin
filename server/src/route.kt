class RouteRequest private constructor (way_points: MutableList <RouteRequest.WayPoint> = mutableListOf()) {
  var way_points : MutableList <RouteRequest.WayPoint>
    private set


  init {
    this.way_points = way_points
  }
  class WayPoint private constructor (distance: Double = 0.0, angle_delta: Double = 0.0) {
    var distance : Double
      private set

    var angle_delta : Double
      private set


    init {
      this.distance = distance
      this.angle_delta = angle_delta
    }

    fun writeTo (output: CodedOutputStream) {
      if (distance != 0.0) {
        output.writeDouble (2, distance)
      }
      if (angle_delta != 0.0) {
        output.writeDouble (3, angle_delta)
      }
    }

    class BuilderWayPoint constructor (distance: Double = 0.0, angle_delta: Double = 0.0) {
      var distance : Double
        private set
      fun setDistance(value: Double): RouteRequest.WayPoint.BuilderWayPoint {
        distance = value
        return this
      }

      var angle_delta : Double
        private set
      fun setAngle_delta(value: Double): RouteRequest.WayPoint.BuilderWayPoint {
        angle_delta = value
        return this
      }


      init {
        this.distance = distance
        this.angle_delta = angle_delta
      }

      fun writeTo (output: CodedOutputStream) {
        if (distance != 0.0) {
          output.writeDouble (2, distance)
        }
        if (angle_delta != 0.0) {
          output.writeDouble (3, angle_delta)
        }
      }

      fun build(): RouteRequest.WayPoint {
        return RouteRequest.WayPoint(distance, angle_delta)
      }

      fun parseFieldFrom(input: CodedInputStream): Boolean {
        if (input.isAtEnd()) { return false }
        val tag = input.readInt32NoTag()
        if (tag == 0) { return false } 
        val fieldNumber = WireFormat.getTagFieldNumber(tag)
        val wireType = WireFormat.getTagWireType(tag)
        when(fieldNumber) {
          2 -> {
            if (wireType != WireType.FIX_64) {
              throw InvalidProtocolBufferException("Error: Field number 2 has wire type WireType.FIX_64 but read ${wireType.toString()}")}
            distance = input.readDoubleNoTag()
          }
          3 -> {
            if (wireType != WireType.FIX_64) {
              throw InvalidProtocolBufferException("Error: Field number 3 has wire type WireType.FIX_64 but read ${wireType.toString()}")}
            angle_delta = input.readDoubleNoTag()
          }
        }
        return true}
      fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): RouteRequest.WayPoint.BuilderWayPoint {
        while(getSizeNoTag() < expectedSize) {
          parseFieldFrom(input)
        }
        if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
        return this
      }
      fun parseFrom(input: CodedInputStream): RouteRequest.WayPoint.BuilderWayPoint {
        while(parseFieldFrom(input)) {}
        return this
      }
      fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (distance != 0.0) {
          size += WireFormat.getDoubleSize(2, distance)
        }
        if (angle_delta != 0.0) {
          size += WireFormat.getDoubleSize(3, angle_delta)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
      }
      fun getSizeNoTag(): Int {
        var size = 0
        if (distance != 0.0) {
          size += WireFormat.getDoubleSize(2, distance)
        }
        if (angle_delta != 0.0) {
          size += WireFormat.getDoubleSize(3, angle_delta)
        }
        return size
      }
    }


    fun mergeWith (other: RouteRequest.WayPoint) {
      distance = other.distance
      angle_delta = other.angle_delta
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
      val builder = RouteRequest.WayPoint.BuilderWayPoint()
      mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

    fun mergeFrom (input: CodedInputStream) {
      val builder = RouteRequest.WayPoint.BuilderWayPoint()
      mergeWith(builder.parseFrom(input).build())}
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (distance != 0.0) {
        size += WireFormat.getDoubleSize(2, distance)
      }
      if (angle_delta != 0.0) {
        size += WireFormat.getDoubleSize(3, angle_delta)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }
    fun getSizeNoTag(): Int {
      var size = 0
      if (distance != 0.0) {
        size += WireFormat.getDoubleSize(2, distance)
      }
      if (angle_delta != 0.0) {
        size += WireFormat.getDoubleSize(3, angle_delta)
      }
      return size
    }
  }


  fun writeTo (output: CodedOutputStream) {
    if (way_points.size > 0) {
      for (item in way_points) {
        if (item != RouteRequest.WayPoint.BuilderWayPoint().build()) {
          output.writeTag(1, WireType.LENGTH_DELIMITED)
          output.writeInt32NoTag(item.getSizeNoTag())
          item.writeTo(output)
        }
      }
    }
  }

  class BuilderRouteRequest constructor (way_points: MutableList <RouteRequest.WayPoint> = mutableListOf()) {
    var way_points : MutableList <RouteRequest.WayPoint>
      private set
    fun setWay_points(value: MutableList <RouteRequest.WayPoint>): RouteRequest.BuilderRouteRequest {
      way_points = value
      return this
    }
    fun setWayPoint(index: Int, value: WayPoint): RouteRequest.BuilderRouteRequest {
      way_points[index] = value
      return this
    }
    fun addWayPoint(value: WayPoint): RouteRequest.BuilderRouteRequest {
      way_points.add(value)
      return this
    }
    fun addAllWayPoint(value: Iterable<WayPoint>): RouteRequest.BuilderRouteRequest {
      for (item in value) {
        way_points.add(item)
      }
      return this
    }


    init {
      this.way_points = way_points
    }

    fun writeTo (output: CodedOutputStream) {
      if (way_points.size > 0) {
        for (item in way_points) {
          if (item != RouteRequest.WayPoint.BuilderWayPoint().build()) {
            output.writeTag(1, WireType.LENGTH_DELIMITED)
            output.writeInt32NoTag(item.getSizeNoTag())
            item.writeTo(output)
          }
        }
      }
    }

    fun build(): RouteRequest {
      return RouteRequest(way_points)
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
          var readSize = 0
          var tmp: RouteRequest.WayPoint = RouteRequest.WayPoint.BuilderWayPoint().build()
          run {
            val expectedSize = input.readInt32NoTag()
            tmp.mergeFromWithSize(input, expectedSize)
            if (expectedSize != tmp.getSizeNoTag()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${tmp.getSizeNoTag()}") }
          }
          if (tmp != RouteRequest.WayPoint.BuilderWayPoint().build()) {
            readSize += tmp.getSizeNoTag()
          }
          way_points.add(tmp)
        }
      }
      return true}
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): RouteRequest.BuilderRouteRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): RouteRequest.BuilderRouteRequest {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (way_points.size != 0) {
        run {
          var arraySize = 0
          for (item in way_points) {
            if (item != RouteRequest.WayPoint.BuilderWayPoint().build()) {
              arraySize += item.getSize(1)
            }
          }
          size += arraySize
        }
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }
    fun getSizeNoTag(): Int {
      var size = 0
      if (way_points.size != 0) {
        run {
          var arraySize = 0
          for (item in way_points) {
            if (item != RouteRequest.WayPoint.BuilderWayPoint().build()) {
              arraySize += item.getSize(1)
            }
          }
          size += arraySize
        }
      }
      return size
    }
  }


  fun mergeWith (other: RouteRequest) {
    way_points.addAll(other.way_points)
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = RouteRequest.BuilderRouteRequest()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = RouteRequest.BuilderRouteRequest()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (way_points.size != 0) {
      run {
        var arraySize = 0
        for (item in way_points) {
          if (item != RouteRequest.WayPoint.BuilderWayPoint().build()) {
            arraySize += item.getSize(1)
          }
        }
        size += arraySize
      }
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }
  fun getSizeNoTag(): Int {
    var size = 0
    if (way_points.size != 0) {
      run {
        var arraySize = 0
        for (item in way_points) {
          if (item != RouteRequest.WayPoint.BuilderWayPoint().build()) {
            arraySize += item.getSize(1)
          }
        }
        size += arraySize
      }
    }
    return size
  }
}


class RouteResponse private constructor (code: Int = 0, errorMsg: String = "") {
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

  class BuilderRouteResponse constructor (code: Int = 0, errorMsg: String = "") {
    var code : Int
      private set
    fun setCode(value: Int): RouteResponse.BuilderRouteResponse {
      code = value
      return this
    }

    var errorMsg : String
      private set
    fun setErrorMsg(value: String): RouteResponse.BuilderRouteResponse {
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

    fun build(): RouteResponse {
      return RouteResponse(code, errorMsg)
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
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): RouteResponse.BuilderRouteResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): RouteResponse.BuilderRouteResponse {
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


  fun mergeWith (other: RouteResponse) {
    code = other.code
    errorMsg = other.errorMsg
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = RouteResponse.BuilderRouteResponse()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = RouteResponse.BuilderRouteResponse()
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


