fun reproduceAccessorsBug() {
    val accessorsBug = AccessorsBug()
    accessorsBug.sm<caret>th = 1
    accessorsBug.setSmth(2)
}