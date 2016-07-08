/**
 * Created by user on 7/6/16.
 */


object WireFormat {
    val TAG_TYPE_BITS: Int = 3
    val TAG_TYPE_MASK: Int = (1 shl TAG_TYPE_BITS) - 1

    fun getTagWireType(tag: Int): WireType {
        return WireType from (tag and TAG_TYPE_MASK).toByte()
    }

    fun getTagFieldNumber(tag: Int): Int {
        return tag ushr TAG_TYPE_BITS
    }
}