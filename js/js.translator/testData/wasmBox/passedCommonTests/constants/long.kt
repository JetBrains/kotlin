fun box(): String {
    if (1L != 1.toLong()) return "fail 1"
    if (0x1L != 0x1.toLong()) return "fail 2"
    if (0X1L != 0X1.toLong()) return "fail 3"
    if (0b1L != 0b1.toLong()) return "fail 4"
    if (0B1L != 0B1.toLong()) return "fail 5"

    return "OK"
}

