package foo

fun box(): String {

    assertEquals(true, 10.0 < 20L, "Double.compareTo(Long)")
    assertEquals(true, 10.0 < 7540113804746346429L, "Double.compareTo(Long)")

    assertEquals(true, 10.0f < 20L, "Float.compareTo(Long)")
    assertEquals(true, 10.0f < 7540113804746346429L, "Float.compareTo(Long)")

    assertEquals(true, 10L < 20L, "Long.compareTo(Long)")
    assertEquals(true, 10 < 20L, "Int.compareTo(Long)")
    assertEquals(true, 10 < 7540113804746346429L, "Int.compareTo(Long)")

    assertEquals(true, (10: Short) < 20L, "Short.compareTo(Long)")
    assertEquals(true, (10: Short) < 7540113804746346429L, "Short.compareTo(Long)")
    assertEquals(true, (10: Byte) < 20L, "Byte.compareTo(Long)")
    assertEquals(true, (10: Byte) < 7540113804746346429L, "Byte.compareTo(Long)")

    assertEquals(true, 10L < 20.0, "Long.compareTo(Double)")
    assertEquals(false, 7540113804746346429L < 20.0, "Long.compareTo(Double)")

    assertEquals(true, 10L < 20.0f, "Long.compareTo(Float)")
    assertEquals(true, 7540113804746346429L > 20.0f, "Long.compareTo(Float)")
    assertEquals(false, 7540113804746346429L < 20.0f, "Long.compareTo(Float)")

    assertEquals(true, 10L < 20, "Long.compareTo(Int)")
    assertEquals(true, 7540113804746346429L > 20, "Long.compareTo(Int)")
    assertEquals(false, 7540113804746346429L < 20, "Long.compareTo(Int)")

    assertEquals(true, 10L < (20: Short), "Long.compareTo(Short)")
    assertEquals(true, 7540113804746346429L > (20: Short), "Long.compareTo(Short)")
    assertEquals(false, 7540113804746346429L < (20: Short), "Long.compareTo(Short)")

    assertEquals(true, 10L < (20: Byte), "Long.compareTo(Byte)")
    assertEquals(true, 7540113804746346429L > (20: Byte), "Long.compareTo(Byte)")
    assertEquals(false, 7540113804746346429L < (20: Byte), "Long.compareTo(Byte)")

    return "OK"
}