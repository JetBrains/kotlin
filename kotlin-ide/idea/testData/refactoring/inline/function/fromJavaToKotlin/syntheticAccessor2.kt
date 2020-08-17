fun reproduceAccessorsBug() {
    val accessorsBug = AccessorsBug()
    accessorsBug.smth = 2 // KT-41030
    accessorsBug.se<caret>tSmth(2)
}