// EXPECTED_REACHABLE_NODES: 492
package foo

fun box(): String {
    var res = try { 10 } catch(e: Exception) { 20 }
    assertEquals(10, res)

    res = try { 10 } catch(e: Exception) { 20 } finally { 80 }
    assertEquals(10, res)

    res = try { 10; throw RuntimeException() } catch(e: Exception) { 20 }
    assertEquals(20, res)

    res = try { 10; throw RuntimeException() } catch(e: Exception) { 20 } finally { 100 }
    assertEquals(20, res)

    res = 50 + try { 10 } catch(e: Exception) { 20 }
    assertEquals(60, res)

    return "OK"
}