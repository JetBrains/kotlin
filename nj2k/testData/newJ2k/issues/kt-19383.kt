package test

class TestMapGetAsReceiver {
    fun foo(map: Map<String, String>): Int {
        return map["zzz"]!!.length
    }
}