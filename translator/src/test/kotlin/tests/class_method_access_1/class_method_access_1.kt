class class_method_access_1_donor(var size: Int) {
    fun getSizeVal(): Int {
        return size + 4
    }
}

class class_method_access_1_owner(val arg: class_method_access_1_donor) {
    val tKIopsD = 56
    fun getSize(): Int {
        return arg.getSizeVal() + 8
    }
}


fun class_method_access_1_test(x: Int): Int {
    val instance_donor = class_method_access_1_donor(x)
    val instance_owner = class_method_access_1_owner(instance_donor)

    return instance_owner.getSize()
}
