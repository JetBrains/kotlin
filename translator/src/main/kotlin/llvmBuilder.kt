package hello

class llvmBuilder{
    private val llvmCode : StringBuilder = StringBuilder()
    class llvmBuilder constructor() {}

    fun addLlvmCode(code : String){
        llvmCode.appendln(code)
    }

    override fun toString(): String {
        return llvmCode.toString()
    }
}