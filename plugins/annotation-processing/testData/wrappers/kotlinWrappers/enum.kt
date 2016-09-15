// FQNAME: EnumClass

enum class EnumClass {
    RED, GREEN, BLUE;
    
    fun someFun() {
        System.out.println("Hello, world!")
    }
    
    fun stringRepresentation() = this.toString()
}