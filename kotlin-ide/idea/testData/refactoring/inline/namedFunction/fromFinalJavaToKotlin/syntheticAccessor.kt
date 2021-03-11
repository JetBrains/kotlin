fun reproduceAccessorsBug() {
    val accessorsBug = AccessorsBug()
    val smth = accessorsBug.smth
    val smth1 = accessorsBug.getSmth()
}