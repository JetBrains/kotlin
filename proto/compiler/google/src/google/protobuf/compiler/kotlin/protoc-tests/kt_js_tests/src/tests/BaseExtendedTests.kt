//package tests
//import Base
//import Extended
//
//
//
//object BaseExtendedTest {
//    fun generateKtBaseMessage(): Base {
//        val arrSize = Util.nextInt(1000)
//        val arr = LongArray(arrSize)
//        for (i in 0..(arrSize - 1)) {
//            arr[i] = Util.nextLong()
//        }
//
//        val flag = if (Util.nextInt() % 2 == 0) false else true
//
//        val int = Util.nextInt()
//
//        return Base.BuilderBase(arr, flag, int).build()
//    }
//
//    fun generateKtExtendedMessage(): Extended {
//        val arrSize = Util.nextInt(1000)
//        val arr = LongArray(arrSize)
//        for (i in 0..(arrSize - 1)) {
//            arr[i] = Util.nextLong()
//        }
//
//        val flag = if (Util.nextInt() % 2 == 0) false else true
//
//        val int = Util.nextInt()
//
//        val long = Util.nextLong()
//
//        val longsSize = Util.nextInt(1000)
//        val longs = LongArray(longsSize)
//        for (i in 0..(longsSize - 1)) {
//            longs[i] = Util.nextLong()
//        }
//
//        return Extended.BuilderExtended(arr, longs, flag, long, int).build()
//    }
//
//    fun generateJvBaseMessage(): BaseMessage.Base {
//        val arrSize = RandomGen.rnd.nextInt(1000)
//        val arr = LongArray(arrSize)
//        for (i in 0..(arrSize - 1)) {
//            arr[i] = RandomGen.rnd.nextLong()
//        }
//
//        val flag = if (RandomGen.rnd.nextInt() % 2 == 0) false else true
//
//        val int = RandomGen.rnd.nextInt()
//
//        return BaseMessage.Base.newBuilder()
//                .addAllArr(arr.asIterable())
//                .setFlag(flag)
//                .setInt(int)
//                .build()
//    }
//
//    fun generateJvExtendedMessage(): ExtendedMessage.Extended {
//        val arrSize = RandomGen.rnd.nextInt(1000)
//        val arr = LongArray(arrSize)
//        for (i in 0..(arrSize - 1)) {
//            arr[i] = RandomGen.rnd.nextLong()
//        }
//
//        val flag = if (RandomGen.rnd.nextInt() % 2 == 0) false else true
//
//        val int = RandomGen.rnd.nextInt()
//
//        val long = RandomGen.rnd.nextLong()
//
//        val longsSize = RandomGen.rnd.nextInt(1000)
//        val longs = LongArray(longsSize)
//        for (i in 0..(longsSize - 1)) {
//            longs[i] = RandomGen.rnd.nextLong()
//        }
//
//        return ExtendedMessage.Extended.newBuilder()
//                .addAllArr(arr.asIterable())
//                .addAllArrLongs(longs.asIterable())
//                .setFlag(flag)
//                .setInt(int)
//                .setLong(long)
//                .build()
//    }
//
//    fun compareBases(kt1: Base, jv: BaseMessage.Base): Boolean {
//        return kt1.flag == jv.flag &&
//                kt1.int == jv.int &&
//                Util.compareArrays(kt1.arr.asIterable(), jv.arrList)
//    }
//
//    fun compareExtended(kt1: Extended, jv: ExtendedMessage.Extended): Boolean {
//        return kt1.flag == jv.flag &&
//                kt1.int == jv.int &&
//                Util.compareArrays(kt1.arr.asIterable(), jv.arrList) &&
//                Util.compareArrays(kt1.arr_longs.asIterable(), jv.arrLongsList) &&
//                kt1.long == jv.long
//    }
//
//    fun compareBaseKtToJavaExtended(kt1: Base, jv: ExtendedMessage.Extended): Boolean {
//        return kt1.flag == jv.flag &&
//                kt1.int == jv.int &&
//                Util.compareArrays(kt1.arr.asIterable(), jv.arrList)
//    }
//
//    fun compareExtendedKtToJavaBase(kt1: Extended, jv: BaseMessage.Base): Boolean {
//        return kt1.flag == jv.flag &&
//                kt1.int == jv.int &&
//                Util.compareArrays(kt1.arr.asIterable(), jv.arrList)
//    }
//
//    fun baseKtToBaseJavaOnce() {
//        val kt1 = generateKtBaseMessage()
//        val outs = Util.getKtOutputStream(kt1.getSizeNoTag())
//        kt1.writeTo(outs)
//
//        val ins = Util.KtOutputStreamToInputStream(outs)
//        val jv = BaseMessage.Base.parseFrom(ins)
//
//        Util.assert(kt1.errorCode == 0)
//        Util.assert(compareBases(kt1, jv))
//    }
//
//    fun baseJavaToBaseKtOnce() {
//        val outs = ByteArrayOutputStream(100000)
//
//        val jv = generateJvBaseMessage()
//        jv.writeTo(outs)
//        val ins = CodedInputStream(outs.toByteArray())
//        val kt1 = Base.BuilderBase(LongArray(0), false, 0).build()
//        kt1.mergeFrom(ins)
//
//        Util.assert(kt1.errorCode == 0)
//        Util.assert(compareBases(kt1, jv))
//    }
//
//    fun baseKtToExtendedJavaOnce() {
//        val kt1 = generateKtBaseMessage()
//        val outs = Util.getKtOutputStream(kt1.getSizeNoTag())
//        kt1.writeTo(outs)
//
//        val ins = Util.KtOutputStreamToInputStream(outs)
//        val jv = ExtendedMessage.Extended.parseFrom(ins)
//
//        Util.assert(kt1.errorCode == 0)
//        Util.assert(compareBaseKtToJavaExtended(kt1, jv))
//    }
//
//    fun extendedJavaToBaseKtOnce() {
//        val outs = ByteArrayOutputStream(100000)
//
//        val jv = generateJvExtendedMessage()
//        jv.writeTo(outs)
//
//        val ins = CodedInputStream(outs.toByteArray())
//
//        val kt1 = Base.BuilderBase(LongArray(0), false, 0).build()
//        kt1.mergeFrom(ins)
//
//        Util.assert(kt1.errorCode == 0)
//        Util.assert(compareBaseKtToJavaExtended(kt1, jv))
//    }
//
//    fun extendedKtToBaseJavaOnce() {
//        val kt1 = generateKtExtendedMessage()
//        val outs = Util.getKtOutputStream(kt1.getSizeNoTag())
//        kt1.writeTo(outs)
//
//        val ins = Util.KtOutputStreamToInputStream(outs)
//        val jv = BaseMessage.Base.parseFrom(ins)
//
//        Util.assert(kt1.errorCode == 0)
//        Util.assert(compareExtendedKtToJavaBase(kt1, jv))
//    }
//
//    fun baseJavaToExtendedKtOnce() {
//        val outs = ByteArrayOutputStream(100000)
//
//        val jv = generateJvBaseMessage()
//        jv.writeTo(outs)
//
//        val ins = CodedInputStream(outs.toByteArray())
//
//        val kt1 = Extended.BuilderExtended(LongArray(0), LongArray(0), false, 0L, 0).build()
//        kt1.mergeFrom(ins)
//
//        Util.assert(kt1.errorCode == 0)
//        Util.assert(compareExtendedKtToJavaBase(kt1, jv))
//    }
//
//    fun extendedKtToExtendedJavaOnce() {
//        val kt1 = generateKtExtendedMessage()
//        val outs = Util.getKtOutputStream(kt1.getSizeNoTag())
//        kt1.writeTo(outs)
//
//        val ins = Util.KtOutputStreamToInputStream(outs)
//        val jv = ExtendedMessage.Extended.parseFrom(ins)
//
//        Util.assert(kt1.errorCode == 0)
//        Util.assert(compareExtended(kt1, jv))
//    }
//
//    fun extendedJavaToExtendedKtOnce() {
//        val outs = ByteArrayOutputStream(100000)
//
//        val jv = generateJvExtendedMessage()
//        jv.writeTo(outs)
//        val ins = CodedInputStream(outs.toByteArray())
//        val kt1 = Extended.BuilderExtended(LongArray(0), LongArray(0), false, 0L, 0).build()
//        kt1.mergeFrom(ins)
//
//        Util.assert(kt1.errorCode == 0)
//        Util.assert(compareExtended(kt1, jv))
//    }
//
//    val testRuns = 1000
//    fun runTests() {
//        for (i in 0..testRuns) {
//            // base - base
//            baseJavaToBaseKtOnce()
//            baseKtToBaseJavaOnce()
//
//            // base - extended
//            baseKtToExtendedJavaOnce()
//            // extendedJavaToBaseKtOnce() - currently failing, proper parsing of unknown fields is needed.
//
//            // extended - base
//            baseJavaToExtendedKtOnce()
//            extendedKtToBaseJavaOnce()
//
//            // extended - extended
//            extendedJavaToExtendedKtOnce()
//            extendedKtToExtendedJavaOnce()
//        }
//    }
//}