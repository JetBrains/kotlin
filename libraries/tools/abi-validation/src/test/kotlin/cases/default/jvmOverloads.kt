@file:Suppress("UNUSED_PARAMETER")

package cases.default


/* JvmOverloads are not supported by metadata reader


@JvmOverloads
public fun publicFunWithOverloads(a: Int = 0, b: String? = null) {}

@JvmOverloads
internal fun internalFunWithOverloads(a: Int = 0, b: String? = null) {}

public class JvmOverloadsClass
@JvmOverloads
internal constructor(val a: Int = 0, val b: String? = null) {

    @JvmOverloads
    internal fun internalFunWithOverloads(a: Int = 0, b: String? = null) {}

}

*/

/* expected:

public final class cases/default/JvmOverloadsClass {
	public final fun getA ()I
	public final fun getB ()Ljava/lang/String;
}

public final class cases/default/JvmOverloadsKt {
	public static final fun publicFunWithOverloads ()V
	public static final fun publicFunWithOverloads (I)V
	public static final fun publicFunWithOverloads (ILjava/lang/String;)V
	public static synthetic fun publicFunWithOverloads$default (ILjava/lang/String;ILjava/lang/Object;)V
}

 */