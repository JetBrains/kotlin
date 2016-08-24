class CrossBranch private constructor () {
    //========== Properties ===========
    var errorCode: Int = 0

    //========== Nested classes declarations ===========
    class Grandfather private constructor (var rf: CrossBranch.Grandfather.RightFather) {
        //========== Properties ===========
        //message rf = 1

        var errorCode: Int = 0

        //========== Nested classes declarations ===========
        class LeftFather private constructor () {
            //========== Properties ===========
            var errorCode: Int = 0

            //========== Nested classes declarations ===========
            class LeftLeftSon private constructor (var son_field: Int) {
                //========== Properties ===========
                //int32 son_field = 1

                var errorCode: Int = 0

                //========== Serialization methods ===========
                fun writeTo (output: CodedOutputStream) {
                    //int32 son_field = 1
                    if (son_field != 0) {
                        output.writeInt32 (1, son_field)
                    }

                }

                fun mergeWith (other: CrossBranch.Grandfather.LeftFather.LeftLeftSon) {
                    son_field = other.son_field
                    this.errorCode = other.errorCode
                }

                fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
                    val builder = CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0)
                    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
                }

                fun mergeFrom (input: CodedInputStream) {
                    val builder = CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0)
                    mergeWith(builder.parseFrom(input).build())
                }

                //========== Size-related methods ===========
                fun getSize(fieldNumber: Int): Int {
                    var size = 0
                    if (son_field != 0) {
                        size += WireFormat.getInt32Size(1, son_field)
                    }
                    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                    return size
                }

                fun getSizeNoTag(): Int {
                    var size = 0
                    if (son_field != 0) {
                        size += WireFormat.getInt32Size(1, son_field)
                    }
                    return size
                }

                //========== Builder ===========
                class BuilderLeftLeftSon constructor (var son_field: Int) {
                    //========== Properties ===========
                    //int32 son_field = 1
                    fun setSon_field(value: Int): CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon {
                        son_field = value
                        return this
                    }

                    var errorCode: Int = 0

                    //========== Serialization methods ===========
                    fun writeTo (output: CodedOutputStream) {
                        //int32 son_field = 1
                        if (son_field != 0) {
                            output.writeInt32 (1, son_field)
                        }

                    }

                    //========== Mutating methods ===========
                    fun build(): CrossBranch.Grandfather.LeftFather.LeftLeftSon {
                        val res = CrossBranch.Grandfather.LeftFather.LeftLeftSon(son_field)
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
                                if (wireType.id != WireType.VARINT.id) {
                                    errorCode = 1
                                    return false
                                }
                                son_field = input.readInt32NoTag()
                            }
                            else -> errorCode = 4
                        }
                        return true
                    }

                    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon {
                        while(getSizeNoTag() < expectedSize) {
                            parseFieldFrom(input)
                        }
                        if (getSizeNoTag() > expectedSize) { errorCode = 2 }
                        return this
                    }

                    fun parseFrom(input: CodedInputStream): CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon {
                        while(parseFieldFrom(input)) {}
                        return this
                    }

                    //========== Size-related methods ===========
                    fun getSize(fieldNumber: Int): Int {
                        var size = 0
                        if (son_field != 0) {
                            size += WireFormat.getInt32Size(1, son_field)
                        }
                        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                        return size
                    }

                    fun getSizeNoTag(): Int {
                        var size = 0
                        if (son_field != 0) {
                            size += WireFormat.getInt32Size(1, son_field)
                        }
                        return size
                    }

                }

            }

            class LeftRightSon private constructor (var son_field: Int) {
                //========== Properties ===========
                //int32 son_field = 1

                var errorCode: Int = 0

                //========== Serialization methods ===========
                fun writeTo (output: CodedOutputStream) {
                    //int32 son_field = 1
                    if (son_field != 0) {
                        output.writeInt32 (1, son_field)
                    }

                }

                fun mergeWith (other: CrossBranch.Grandfather.LeftFather.LeftRightSon) {
                    son_field = other.son_field
                    this.errorCode = other.errorCode
                }

                fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
                    val builder = CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0)
                    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
                }

                fun mergeFrom (input: CodedInputStream) {
                    val builder = CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0)
                    mergeWith(builder.parseFrom(input).build())
                }

                //========== Size-related methods ===========
                fun getSize(fieldNumber: Int): Int {
                    var size = 0
                    if (son_field != 0) {
                        size += WireFormat.getInt32Size(1, son_field)
                    }
                    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                    return size
                }

                fun getSizeNoTag(): Int {
                    var size = 0
                    if (son_field != 0) {
                        size += WireFormat.getInt32Size(1, son_field)
                    }
                    return size
                }

                //========== Builder ===========
                class BuilderLeftRightSon constructor (var son_field: Int) {
                    //========== Properties ===========
                    //int32 son_field = 1
                    fun setSon_field(value: Int): CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon {
                        son_field = value
                        return this
                    }

                    var errorCode: Int = 0

                    //========== Serialization methods ===========
                    fun writeTo (output: CodedOutputStream) {
                        //int32 son_field = 1
                        if (son_field != 0) {
                            output.writeInt32 (1, son_field)
                        }

                    }

                    //========== Mutating methods ===========
                    fun build(): CrossBranch.Grandfather.LeftFather.LeftRightSon {
                        val res = CrossBranch.Grandfather.LeftFather.LeftRightSon(son_field)
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
                                if (wireType.id != WireType.VARINT.id) {
                                    errorCode = 1
                                    return false
                                }
                                son_field = input.readInt32NoTag()
                            }
                            else -> errorCode = 4
                        }
                        return true
                    }

                    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon {
                        while(getSizeNoTag() < expectedSize) {
                            parseFieldFrom(input)
                        }
                        if (getSizeNoTag() > expectedSize) { errorCode = 2 }
                        return this
                    }

                    fun parseFrom(input: CodedInputStream): CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon {
                        while(parseFieldFrom(input)) {}
                        return this
                    }

                    //========== Size-related methods ===========
                    fun getSize(fieldNumber: Int): Int {
                        var size = 0
                        if (son_field != 0) {
                            size += WireFormat.getInt32Size(1, son_field)
                        }
                        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                        return size
                    }

                    fun getSizeNoTag(): Int {
                        var size = 0
                        if (son_field != 0) {
                            size += WireFormat.getInt32Size(1, son_field)
                        }
                        return size
                    }

                }

            }

            //========== Serialization methods ===========
            fun writeTo (output: CodedOutputStream) {
            }

            fun mergeWith (other: CrossBranch.Grandfather.LeftFather) {
                this.errorCode = other.errorCode
            }

            fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
                val builder = CrossBranch.Grandfather.LeftFather.BuilderLeftFather()
                mergeWith(builder.parseFromWithSize(input, expectedSize).build())
            }

            fun mergeFrom (input: CodedInputStream) {
                val builder = CrossBranch.Grandfather.LeftFather.BuilderLeftFather()
                mergeWith(builder.parseFrom(input).build())
            }

            //========== Size-related methods ===========
            fun getSize(fieldNumber: Int): Int {
                var size = 0
                size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                return size
            }

            fun getSizeNoTag(): Int {
                val size = 0
                return size
            }

            //========== Builder ===========
            class BuilderLeftFather constructor () {
                //========== Properties ===========
                var errorCode: Int = 0

                //========== Serialization methods ===========
                fun writeTo (output: CodedOutputStream) {
                }

                //========== Mutating methods ===========
                fun build(): CrossBranch.Grandfather.LeftFather {
                    val res = CrossBranch.Grandfather.LeftFather()
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
                        else -> errorCode = 4
                    }
                    return true
                }

                fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): CrossBranch.Grandfather.LeftFather.BuilderLeftFather {
                    while(getSizeNoTag() < expectedSize) {
                        parseFieldFrom(input)
                    }
                    if (getSizeNoTag() > expectedSize) { errorCode = 2 }
                    return this
                }

                fun parseFrom(input: CodedInputStream): CrossBranch.Grandfather.LeftFather.BuilderLeftFather {
                    while(parseFieldFrom(input)) {}
                    return this
                }

                //========== Size-related methods ===========
                fun getSize(fieldNumber: Int): Int {
                    var size = 0
                    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                    return size
                }

                fun getSizeNoTag(): Int {
                    val size = 0
                    return size
                }

            }

        }

        class RightFather private constructor (var rls: CrossBranch.Grandfather.RightFather.RightLeftSon, var rrs: CrossBranch.Grandfather.RightFather.RightRightSon) {
            //========== Properties ===========
            //message rls = 1

            //message rrs = 2

            var errorCode: Int = 0

            //========== Nested classes declarations ===========
            class RightLeftSon private constructor (var son_field: CrossBranch.Grandfather.LeftFather.LeftLeftSon) {
                //========== Properties ===========
                //message son_field = 1

                var errorCode: Int = 0

                //========== Serialization methods ===========
                fun writeTo (output: CodedOutputStream) {
                    //message son_field = 1
                    output.writeTag(1, WireType.LENGTH_DELIMITED)
                    output.writeInt32NoTag(son_field.getSizeNoTag())
                    son_field.writeTo(output)

                }

                fun mergeWith (other: CrossBranch.Grandfather.RightFather.RightLeftSon) {
                    son_field = other.son_field
                    this.errorCode = other.errorCode
                }

                fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
                    val builder = CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build())
                    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
                }

                fun mergeFrom (input: CodedInputStream) {
                    val builder = CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build())
                    mergeWith(builder.parseFrom(input).build())
                }

                //========== Size-related methods ===========
                fun getSize(fieldNumber: Int): Int {
                    var size = 0
                    if (son_field != CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()) {
                        size += son_field.getSize(1)
                    }
                    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                    return size
                }

                fun getSizeNoTag(): Int {
                    var size = 0
                    if (son_field != CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()) {
                        size += son_field.getSize(1)
                    }
                    return size
                }

                //========== Builder ===========
                class BuilderRightLeftSon constructor (var son_field: CrossBranch.Grandfather.LeftFather.LeftLeftSon) {
                    //========== Properties ===========
                    //message son_field = 1
                    fun setSon_field(value: CrossBranch.Grandfather.LeftFather.LeftLeftSon): CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon {
                        son_field = value
                        return this
                    }

                    var errorCode: Int = 0

                    //========== Serialization methods ===========
                    fun writeTo (output: CodedOutputStream) {
                        //message son_field = 1
                        output.writeTag(1, WireType.LENGTH_DELIMITED)
                        output.writeInt32NoTag(son_field.getSizeNoTag())
                        son_field.writeTo(output)

                    }

                    //========== Mutating methods ===========
                    fun build(): CrossBranch.Grandfather.RightFather.RightLeftSon {
                        val res = CrossBranch.Grandfather.RightFather.RightLeftSon(son_field)
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
                                do {
                                    val expectedSize = input.readInt32NoTag()
                                    son_field.mergeFromWithSize(input, expectedSize)
                                    if (expectedSize != son_field.getSizeNoTag()) { errorCode = 3; return false }
                                } while(false)
                            }
                            else -> errorCode = 4
                        }
                        return true
                    }

                    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon {
                        while(getSizeNoTag() < expectedSize) {
                            parseFieldFrom(input)
                        }
                        if (getSizeNoTag() > expectedSize) { errorCode = 2 }
                        return this
                    }

                    fun parseFrom(input: CodedInputStream): CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon {
                        while(parseFieldFrom(input)) {}
                        return this
                    }

                    //========== Size-related methods ===========
                    fun getSize(fieldNumber: Int): Int {
                        var size = 0
                        if (son_field != CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()) {
                            size += son_field.getSize(1)
                        }
                        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                        return size
                    }

                    fun getSizeNoTag(): Int {
                        var size = 0
                        if (son_field != CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()) {
                            size += son_field.getSize(1)
                        }
                        return size
                    }

                }

            }

            class RightRightSon private constructor (var son_field: CrossBranch.Grandfather.LeftFather.LeftRightSon) {
                //========== Properties ===========
                //message son_field = 1

                var errorCode: Int = 0

                //========== Serialization methods ===========
                fun writeTo (output: CodedOutputStream) {
                    //message son_field = 1
                    output.writeTag(1, WireType.LENGTH_DELIMITED)
                    output.writeInt32NoTag(son_field.getSizeNoTag())
                    son_field.writeTo(output)

                }

                fun mergeWith (other: CrossBranch.Grandfather.RightFather.RightRightSon) {
                    son_field = other.son_field
                    this.errorCode = other.errorCode
                }

                fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
                    val builder = CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build())
                    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
                }

                fun mergeFrom (input: CodedInputStream) {
                    val builder = CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build())
                    mergeWith(builder.parseFrom(input).build())
                }

                //========== Size-related methods ===========
                fun getSize(fieldNumber: Int): Int {
                    var size = 0
                    if (son_field != CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()) {
                        size += son_field.getSize(1)
                    }
                    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                    return size
                }

                fun getSizeNoTag(): Int {
                    var size = 0
                    if (son_field != CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()) {
                        size += son_field.getSize(1)
                    }
                    return size
                }

                //========== Builder ===========
                class BuilderRightRightSon constructor (var son_field: CrossBranch.Grandfather.LeftFather.LeftRightSon) {
                    //========== Properties ===========
                    //message son_field = 1
                    fun setSon_field(value: CrossBranch.Grandfather.LeftFather.LeftRightSon): CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon {
                        son_field = value
                        return this
                    }

                    var errorCode: Int = 0

                    //========== Serialization methods ===========
                    fun writeTo (output: CodedOutputStream) {
                        //message son_field = 1
                        output.writeTag(1, WireType.LENGTH_DELIMITED)
                        output.writeInt32NoTag(son_field.getSizeNoTag())
                        son_field.writeTo(output)

                    }

                    //========== Mutating methods ===========
                    fun build(): CrossBranch.Grandfather.RightFather.RightRightSon {
                        val res = CrossBranch.Grandfather.RightFather.RightRightSon(son_field)
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
                                do {
                                    val expectedSize = input.readInt32NoTag()
                                    son_field.mergeFromWithSize(input, expectedSize)
                                    if (expectedSize != son_field.getSizeNoTag()) { errorCode = 3; return false }
                                } while(false)
                            }
                            else -> errorCode = 4
                        }
                        return true
                    }

                    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon {
                        while(getSizeNoTag() < expectedSize) {
                            parseFieldFrom(input)
                        }
                        if (getSizeNoTag() > expectedSize) { errorCode = 2 }
                        return this
                    }

                    fun parseFrom(input: CodedInputStream): CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon {
                        while(parseFieldFrom(input)) {}
                        return this
                    }

                    //========== Size-related methods ===========
                    fun getSize(fieldNumber: Int): Int {
                        var size = 0
                        if (son_field != CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()) {
                            size += son_field.getSize(1)
                        }
                        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                        return size
                    }

                    fun getSizeNoTag(): Int {
                        var size = 0
                        if (son_field != CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()) {
                            size += son_field.getSize(1)
                        }
                        return size
                    }

                }

            }

            //========== Serialization methods ===========
            fun writeTo (output: CodedOutputStream) {
                //message rls = 1
                output.writeTag(1, WireType.LENGTH_DELIMITED)
                output.writeInt32NoTag(rls.getSizeNoTag())
                rls.writeTo(output)

                //message rrs = 2
                output.writeTag(2, WireType.LENGTH_DELIMITED)
                output.writeInt32NoTag(rrs.getSizeNoTag())
                rrs.writeTo(output)

            }

            fun mergeWith (other: CrossBranch.Grandfather.RightFather) {
                rls = other.rls
                rrs = other.rrs
                this.errorCode = other.errorCode
            }

            fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
                val builder = CrossBranch.Grandfather.RightFather.BuilderRightFather(CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build(), CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build())
                mergeWith(builder.parseFromWithSize(input, expectedSize).build())
            }

            fun mergeFrom (input: CodedInputStream) {
                val builder = CrossBranch.Grandfather.RightFather.BuilderRightFather(CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build(), CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build())
                mergeWith(builder.parseFrom(input).build())
            }

            //========== Size-related methods ===========
            fun getSize(fieldNumber: Int): Int {
                var size = 0
                if (rls != CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build()) {
                    size += rls.getSize(1)
                }
                if (rrs != CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build()) {
                    size += rrs.getSize(2)
                }
                size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                return size
            }

            fun getSizeNoTag(): Int {
                var size = 0
                if (rls != CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build()) {
                    size += rls.getSize(1)
                }
                if (rrs != CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build()) {
                    size += rrs.getSize(2)
                }
                return size
            }

            //========== Builder ===========
            class BuilderRightFather constructor (var rls: CrossBranch.Grandfather.RightFather.RightLeftSon, var rrs: CrossBranch.Grandfather.RightFather.RightRightSon) {
                //========== Properties ===========
                //message rls = 1
                fun setRls(value: CrossBranch.Grandfather.RightFather.RightLeftSon): CrossBranch.Grandfather.RightFather.BuilderRightFather {
                    rls = value
                    return this
                }

                //message rrs = 2
                fun setRrs(value: CrossBranch.Grandfather.RightFather.RightRightSon): CrossBranch.Grandfather.RightFather.BuilderRightFather {
                    rrs = value
                    return this
                }

                var errorCode: Int = 0

                //========== Serialization methods ===========
                fun writeTo (output: CodedOutputStream) {
                    //message rls = 1
                    output.writeTag(1, WireType.LENGTH_DELIMITED)
                    output.writeInt32NoTag(rls.getSizeNoTag())
                    rls.writeTo(output)

                    //message rrs = 2
                    output.writeTag(2, WireType.LENGTH_DELIMITED)
                    output.writeInt32NoTag(rrs.getSizeNoTag())
                    rrs.writeTo(output)

                }

                //========== Mutating methods ===========
                fun build(): CrossBranch.Grandfather.RightFather {
                    val res = CrossBranch.Grandfather.RightFather(rls, rrs)
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
                            do {
                                val expectedSize = input.readInt32NoTag()
                                rls.mergeFromWithSize(input, expectedSize)
                                if (expectedSize != rls.getSizeNoTag()) { errorCode = 3; return false }
                            } while(false)
                        }
                        2 -> {
                            if (wireType.id != WireType.LENGTH_DELIMITED.id) {
                                errorCode = 1
                                return false
                            }
                            do {
                                val expectedSize = input.readInt32NoTag()
                                rrs.mergeFromWithSize(input, expectedSize)
                                if (expectedSize != rrs.getSizeNoTag()) { errorCode = 3; return false }
                            } while(false)
                        }
                        else -> errorCode = 4
                    }
                    return true
                }

                fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): CrossBranch.Grandfather.RightFather.BuilderRightFather {
                    while(getSizeNoTag() < expectedSize) {
                        parseFieldFrom(input)
                    }
                    if (getSizeNoTag() > expectedSize) { errorCode = 2 }
                    return this
                }

                fun parseFrom(input: CodedInputStream): CrossBranch.Grandfather.RightFather.BuilderRightFather {
                    while(parseFieldFrom(input)) {}
                    return this
                }

                //========== Size-related methods ===========
                fun getSize(fieldNumber: Int): Int {
                    var size = 0
                    if (rls != CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build()) {
                        size += rls.getSize(1)
                    }
                    if (rrs != CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build()) {
                        size += rrs.getSize(2)
                    }
                    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                    return size
                }

                fun getSizeNoTag(): Int {
                    var size = 0
                    if (rls != CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build()) {
                        size += rls.getSize(1)
                    }
                    if (rrs != CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build()) {
                        size += rrs.getSize(2)
                    }
                    return size
                }

            }

        }

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //message rf = 1
            output.writeTag(1, WireType.LENGTH_DELIMITED)
            output.writeInt32NoTag(rf.getSizeNoTag())
            rf.writeTo(output)

        }

        fun mergeWith (other: CrossBranch.Grandfather) {
            rf = other.rf
            this.errorCode = other.errorCode
        }

        fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
            val builder = CrossBranch.Grandfather.BuilderGrandfather(CrossBranch.Grandfather.RightFather.BuilderRightFather(CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build(), CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build()).build())
            mergeWith(builder.parseFromWithSize(input, expectedSize).build())
        }

        fun mergeFrom (input: CodedInputStream) {
            val builder = CrossBranch.Grandfather.BuilderGrandfather(CrossBranch.Grandfather.RightFather.BuilderRightFather(CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build(), CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build()).build())
            mergeWith(builder.parseFrom(input).build())
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (rf != CrossBranch.Grandfather.RightFather.BuilderRightFather(CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build(), CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build()).build()) {
                size += rf.getSize(1)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (rf != CrossBranch.Grandfather.RightFather.BuilderRightFather(CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build(), CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build()).build()) {
                size += rf.getSize(1)
            }
            return size
        }

        //========== Builder ===========
        class BuilderGrandfather constructor (var rf: CrossBranch.Grandfather.RightFather) {
            //========== Properties ===========
            //message rf = 1
            fun setRf(value: CrossBranch.Grandfather.RightFather): CrossBranch.Grandfather.BuilderGrandfather {
                rf = value
                return this
            }

            var errorCode: Int = 0

            //========== Serialization methods ===========
            fun writeTo (output: CodedOutputStream) {
                //message rf = 1
                output.writeTag(1, WireType.LENGTH_DELIMITED)
                output.writeInt32NoTag(rf.getSizeNoTag())
                rf.writeTo(output)

            }

            //========== Mutating methods ===========
            fun build(): CrossBranch.Grandfather {
                val res = CrossBranch.Grandfather(rf)
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
                        do {
                            val expectedSize = input.readInt32NoTag()
                            rf.mergeFromWithSize(input, expectedSize)
                            if (expectedSize != rf.getSizeNoTag()) { errorCode = 3; return false }
                        } while(false)
                    }
                    else -> errorCode = 4
                }
                return true
            }

            fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): CrossBranch.Grandfather.BuilderGrandfather {
                while(getSizeNoTag() < expectedSize) {
                    parseFieldFrom(input)
                }
                if (getSizeNoTag() > expectedSize) { errorCode = 2 }
                return this
            }

            fun parseFrom(input: CodedInputStream): CrossBranch.Grandfather.BuilderGrandfather {
                while(parseFieldFrom(input)) {}
                return this
            }

            //========== Size-related methods ===========
            fun getSize(fieldNumber: Int): Int {
                var size = 0
                if (rf != CrossBranch.Grandfather.RightFather.BuilderRightFather(CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build(), CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build()).build()) {
                    size += rf.getSize(1)
                }
                size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                return size
            }

            fun getSizeNoTag(): Int {
                var size = 0
                if (rf != CrossBranch.Grandfather.RightFather.BuilderRightFather(CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(0).build()).build(), CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(0).build()).build()).build()) {
                    size += rf.getSize(1)
                }
                return size
            }

        }

    }

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
    }

    fun mergeWith (other: CrossBranch) {
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = CrossBranch.BuilderCrossBranch()
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = CrossBranch.BuilderCrossBranch()
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        val size = 0
        return size
    }

    //========== Builder ===========
    class BuilderCrossBranch constructor () {
        //========== Properties ===========
        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
        }

        //========== Mutating methods ===========
        fun build(): CrossBranch {
            val res = CrossBranch()
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
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): CrossBranch.BuilderCrossBranch {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): CrossBranch.BuilderCrossBranch {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            val size = 0
            return size
        }

    }

}

fun compareGrandfathers(lhs: CrossBranch.Grandfather, rhs: CrossBranch.Grandfather): Boolean {
    return (lhs.rf.rls.son_field.son_field == rhs.rf.rls.son_field.son_field) and
            (lhs.rf.rrs.son_field.son_field == rhs.rf.rrs.son_field.son_field)
}

fun buildGrandfather(val1: Int, val2: Int): CrossBranch.Grandfather {
    return CrossBranch.Grandfather.BuilderGrandfather(
            CrossBranch.Grandfather.RightFather.BuilderRightFather(
                    CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(
                            CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(
                                    val1
                            ).build()
                    ).build()
                    ,
                    CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(
                            CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(
                                    val2
                            ).build()
                    ).build()
            ).build()
    ).build()
}

fun checkGFSerializationIdentity(msg: CrossBranch.Grandfather): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = buildGrandfather(0, 0)
    readMsg.mergeFrom(ins)

    if (readMsg.errorCode != 0) {
        return 1
    }
    if (compareGrandfathers(msg, readMsg)) {
        return 0
    } else {
        return 1
    }
}

fun testTrivialValues(): Int {
    val msg = buildGrandfather(3213213, -217864283)
    return checkGFSerializationIdentity(msg)
}

fun testMaxValues(): Int {
    val msg = buildGrandfather(2147483647, 2147483647)
    return checkGFSerializationIdentity(msg)
}

fun testMinValues(): Int {
    val msg = buildGrandfather(-2147483647, -2147483647)
    return checkGFSerializationIdentity(msg)
}