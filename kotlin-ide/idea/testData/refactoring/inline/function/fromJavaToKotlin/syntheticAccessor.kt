fun reproduceAccessorsBug() {
    val accessorsBug = AccessorsBug()
    val smth = accessorsBug.smth // KT-41030
    val smth1 = accessorsBug.getSmth()
}