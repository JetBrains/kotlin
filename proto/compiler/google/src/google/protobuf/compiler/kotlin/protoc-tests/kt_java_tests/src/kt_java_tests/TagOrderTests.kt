package kt_java_tests

import java_msg.TagOrderOuterClass
import java_msg.TagOrderShuffledOuterClass
import CodedInputStream
import TagOrder
import TagOrderShuffled
import java.io.ByteArrayOutputStream

object TagOrderTests {
    fun generateKtTag(): TagOrder {
        val int = RandomGen.rnd.nextInt()
        val slong_array = Util.generateLongArray()
        val sint = RandomGen.rnd.nextInt()
        val enum = TagOrder.Enum.fromIntToEnum(RandomGen.rnd.nextInt(2))

        return TagOrder.BuilderTagOrder(int, sint, slong_array, enum).build()
    }

    fun generateKtTagShuffled(): TagOrderShuffled {
        val int = RandomGen.rnd.nextInt()
        val slong_array = Util.generateLongArray()
        val sint = RandomGen.rnd.nextInt()
        val enum = TagOrderShuffled.Enum.fromIntToEnum(RandomGen.rnd.nextInt(2))

        return TagOrderShuffled.BuilderTagOrderShuffled(int, sint, slong_array, enum).build()
    }

    fun generateJvTag(): TagOrderOuterClass.TagOrder {
        val int = RandomGen.rnd.nextInt()
        val slong_array = Util.generateLongArray()
        val sint = RandomGen.rnd.nextInt()
        val enum = TagOrderOuterClass.TagOrder.Enum.forNumber(RandomGen.rnd.nextInt(2))

        return TagOrderOuterClass.TagOrder.newBuilder()
                .setInt(int)
                .addAllSlongArray(slong_array.asIterable())
                .setSint(int)
                .setEnumField(enum)
                .build()
    }

    fun generateJvTagShuffled(): TagOrderShuffledOuterClass.TagOrderShuffled {
        val int = RandomGen.rnd.nextInt()
        val slong_array = Util.generateLongArray()
        val sint = RandomGen.rnd.nextInt()
        val enum = TagOrderShuffledOuterClass.TagOrderShuffled.Enum.forNumber(RandomGen.rnd.nextInt(2))

        return TagOrderShuffledOuterClass.TagOrderShuffled.newBuilder()
                .setInt(int)
                .addAllSlongArray(slong_array.asIterable())
                .setSint(int)
                .setEnumField(enum)
                .build()
    }

    fun compareTagAndShuffledTag(kt: TagOrder, jv: TagOrderShuffledOuterClass.TagOrderShuffled): Boolean {
        return kt.enum_field.id == jv.enumField.number &&
                kt.int == jv.int &&
                kt.sint == jv.sint &&
                Util.compareArrays(kt.slong_array.asIterable(), jv.slongArrayList)
    }

    fun compareShuffledAndTag(kt: TagOrderShuffled, jv: TagOrderOuterClass.TagOrder): Boolean {
        return kt.enum_field.id == jv.enumField.number &&
                kt.int == jv.int &&
                kt.sint == jv.sint &&
                Util.compareArrays(kt.slong_array.asIterable(), jv.slongArrayList)
    }

    fun jvTagToKtShuffledOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jv = generateJvTag()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = TagOrderShuffled.BuilderTagOrderShuffled(
                    0, 0, LongArray(0), TagOrderShuffled.Enum.first_val
                ).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareShuffledAndTag(kt, jv))
    }

    fun jvShuffledToKtTagOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jv = generateJvTagShuffled()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = TagOrder.BuilderTagOrder(
                0, 0, LongArray(0), TagOrder.Enum.first_val
        ).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareTagAndShuffledTag(kt, jv))
    }

    fun ktTagToJvShuffled() {
        val kt = generateKtTag()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())

        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = TagOrderShuffledOuterClass.TagOrderShuffled.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareTagAndShuffledTag(kt, jv))
    }

    fun ktShuffledToJvTag() {
        val kt = generateKtTagShuffled()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())

        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = TagOrderOuterClass.TagOrder.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareShuffledAndTag(kt, jv))
    }

    val testRuns = 1000
    fun runTests() {
        for (i in 0..testRuns) {
            // kt -> java
            ktShuffledToJvTag()
            ktTagToJvShuffled()

            // java -> kt
            jvShuffledToKtTagOnce()
            jvTagToKtShuffledOnce()
        }
    }
}