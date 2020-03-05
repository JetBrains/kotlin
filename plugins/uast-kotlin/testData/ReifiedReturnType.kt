inline fun <T> function1(t: T) {}
inline fun <T> function2(t: T): T = t
inline fun <reified T> function3(t: T) {}
inline fun <reified T> function4(t: T): T = t
inline fun <reified T> function5(t: T): Int = 42
inline fun <reified T : Activity> T.function6(t: T): T = t
inline fun <reified T> function7(t: T): T = t
private inline fun <reified T> function8(t: T): T = t
internal inline fun <reified T> function9(t: T): T = t
public inline fun <reified T> function10(t: T): T = t
inline fun <reified T> T.function11(t: T): T = t