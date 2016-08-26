class RouteRequest private constructor (var distances: IntArray, var angles: IntArray) {
  //========== Properties ===========
  //repeated int32 distances = 1

  //repeated int32 angles = 2

  var errorCode: Int = 0

  //========== Serialization methods ===========
  fun writeTo (output: CodedOutputStream) {
    //repeated int32 distances = 1
    if (distances.size > 0) {
      output.writeTag(1, WireType.LENGTH_DELIMITED)
      var arrayByteSize = 0

      if (distances.size != 0) {
        do {
          var arraySize = 0
          var i = 0
          while (i < distances.size) {
            arraySize += WireFormat.getInt32SizeNoTag(distances[i])
            i += 1
          }
          arrayByteSize += arraySize
        } while(false)
      }
      output.writeInt32NoTag(arrayByteSize)

      do {
        var i = 0
        while (i < distances.size) {
          output.writeInt32NoTag (distances[i])
          i += 1
        }
      } while(false)
    }

    //repeated int32 angles = 2
    if (angles.size > 0) {
      output.writeTag(2, WireType.LENGTH_DELIMITED)
      var arrayByteSize = 0

      if (angles.size != 0) {
        do {
          var arraySize = 0
          var i = 0
          while (i < angles.size) {
            arraySize += WireFormat.getInt32SizeNoTag(angles[i])
            i += 1
          }
          arrayByteSize += arraySize
        } while(false)
      }
      output.writeInt32NoTag(arrayByteSize)

      do {
        var i = 0
        while (i < angles.size) {
          output.writeInt32NoTag (angles[i])
          i += 1
        }
      } while(false)
    }

  }

  fun mergeWith (other: RouteRequest) {
    distances = distances.plus((other.distances))
    angles = angles.plus((other.angles))
    this.errorCode = other.errorCode
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0))
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0))
    mergeWith(builder.parseFrom(input).build())
  }

  //========== Size-related methods ===========
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (distances.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < distances.size) {
          arraySize += WireFormat.getInt32SizeNoTag(distances[i])
          i += 1
        }
        size += arraySize
        size += WireFormat.getInt32SizeNoTag(arraySize)
      } while(false)
    }
    if (angles.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < angles.size) {
          arraySize += WireFormat.getInt32SizeNoTag(angles[i])
          i += 1
        }
        size += arraySize
        size += WireFormat.getInt32SizeNoTag(arraySize)
      } while(false)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }

  fun getSizeNoTag(): Int {
    var size = 0
    if (distances.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < distances.size) {
          arraySize += WireFormat.getInt32SizeNoTag(distances[i])
          i += 1
        }
        size += arraySize
        size += WireFormat.getInt32SizeNoTag(arraySize)
      } while(false)
    }
    if (angles.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < angles.size) {
          arraySize += WireFormat.getInt32SizeNoTag(angles[i])
          i += 1
        }
        size += arraySize
        size += WireFormat.getInt32SizeNoTag(arraySize)
      } while(false)
    }
    return size
  }

  //========== Builder ===========
  class BuilderRouteRequest constructor (var distances: IntArray, var angles: IntArray) {
    //========== Properties ===========
    //repeated int32 distances = 1
    fun setDistances(value: IntArray): RouteRequest.BuilderRouteRequest {
      distances = value
      return this
    }
    fun setdistancesByIndex(index: Int, value: Int): RouteRequest.BuilderRouteRequest {
      distances[index] = value
      return this
    }

    //repeated int32 angles = 2
    fun setAngles(value: IntArray): RouteRequest.BuilderRouteRequest {
      angles = value
      return this
    }
    fun setanglesByIndex(index: Int, value: Int): RouteRequest.BuilderRouteRequest {
      angles[index] = value
      return this
    }

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
      //repeated int32 distances = 1
      if (distances.size > 0) {
        output.writeTag(1, WireType.LENGTH_DELIMITED)
        var arrayByteSize = 0

        if (distances.size != 0) {
          do {
            var arraySize = 0
            var i = 0
            while (i < distances.size) {
              arraySize += WireFormat.getInt32SizeNoTag(distances[i])
              i += 1
            }
            arrayByteSize += arraySize
          } while(false)
        }
        output.writeInt32NoTag(arrayByteSize)

        do {
          var i = 0
          while (i < distances.size) {
            output.writeInt32NoTag (distances[i])
            i += 1
          }
        } while(false)
      }

      //repeated int32 angles = 2
      if (angles.size > 0) {
        output.writeTag(2, WireType.LENGTH_DELIMITED)
        var arrayByteSize = 0

        if (angles.size != 0) {
          do {
            var arraySize = 0
            var i = 0
            while (i < angles.size) {
              arraySize += WireFormat.getInt32SizeNoTag(angles[i])
              i += 1
            }
            arrayByteSize += arraySize
          } while(false)
        }
        output.writeInt32NoTag(arrayByteSize)

        do {
          var i = 0
          while (i < angles.size) {
            output.writeInt32NoTag (angles[i])
            i += 1
          }
        } while(false)
      }

    }

    //========== Mutating methods ===========
    fun build(): RouteRequest {
      val res = RouteRequest(distances, angles)
      res.errorCode = errorCode
      return res
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false }
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> {
          if (wireType.id != WireType.LENGTH_DELIMITED.id) {
            errorCode = 1
            return false
          }
          val expectedByteSize = input.readInt32NoTag()
          var newArray = IntArray(0)
          var readSize = 0
          do {
            var i = 0
            while(readSize < expectedByteSize) {
              val tmp = IntArray(1)
              tmp[0] = input.readInt32NoTag()
              newArray = newArray.plus(tmp)
              readSize += WireFormat.getInt32SizeNoTag(tmp[0])
            }
            distances = newArray
          } while (false)
        }
        2 -> {
          if (wireType.id != WireType.LENGTH_DELIMITED.id) {
            errorCode = 1
            return false
          }
          val expectedByteSize = input.readInt32NoTag()
          var newArray = IntArray(0)
          var readSize = 0
          do {
            var i = 0
            while(readSize < expectedByteSize) {
              val tmp = IntArray(1)
              tmp[0] = input.readInt32NoTag()
              newArray = newArray.plus(tmp)
              readSize += WireFormat.getInt32SizeNoTag(tmp[0])
            }
            angles = newArray
          } while (false)
        }
        else -> errorCode = 4
      }
      return true
    }

    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): RouteRequest.BuilderRouteRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { errorCode = 2 }
      return this
    }

    fun parseFrom(input: CodedInputStream): RouteRequest.BuilderRouteRequest {
      while(parseFieldFrom(input)) {}
      return this
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (distances.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < distances.size) {
            arraySize += WireFormat.getInt32SizeNoTag(distances[i])
            i += 1
          }
          size += arraySize
          size += WireFormat.getInt32SizeNoTag(arraySize)
        } while(false)
      }
      if (angles.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < angles.size) {
            arraySize += WireFormat.getInt32SizeNoTag(angles[i])
            i += 1
          }
          size += arraySize
          size += WireFormat.getInt32SizeNoTag(arraySize)
        } while(false)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }

    fun getSizeNoTag(): Int {
      var size = 0
      if (distances.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < distances.size) {
            arraySize += WireFormat.getInt32SizeNoTag(distances[i])
            i += 1
          }
          size += arraySize
          size += WireFormat.getInt32SizeNoTag(arraySize)
        } while(false)
      }
      if (angles.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < angles.size) {
            arraySize += WireFormat.getInt32SizeNoTag(angles[i])
            i += 1
          }
          size += arraySize
          size += WireFormat.getInt32SizeNoTag(arraySize)
        } while(false)
      }
      return size
    }

  }

}

fun echoProto() {
    var i = 0
    while (i < 100) {
        val route = readRoute()
        go(route)
        i++
    }
}

fun receiveByteArray(): ByteArray {
    val result = ByteArray(10)
    result[0] = 10
    result[1] = 4
    result[2] = 232.toByte()
    result[3] = 7
    result[4] = 232.toByte()
    result[5] = 7
    result[6] = 18
    result[7] = 2
    result[8] = 0
    result[9] = 0

    return result
}

fun readRoute(): RouteRequest {
    val buffer = receiveByteArray()
    val stream = CodedInputStream(buffer)
    val result = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0)).parseFrom(stream).build()

    return result
}

fun wait(i: Int) {
}

fun go(request: RouteRequest) {
    val times = request.distances
    var j = 0

    while (j < times.size) {
        val time = times[j]
        wait(time)
        j++
    }

}