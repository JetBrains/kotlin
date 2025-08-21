package modifiers

public class PublicClass public constructor()

public open class PublicClassProtectedConstructor protected constructor() {
    protected interface ProtectedInterface
    private interface PrivateInterface
}

public abstract class PublicClassPrivateConstructor private constructor()
internal class InternalClass
private class PrivateClass

public interface PublicInterface
internal interface InternalInterface
private interface PrivateInterface

sealed class SealedClass {
    class One : SealedClass()
    open class Two : SealedClass()
    abstract class Three : Two()
    final class Four : Three()
}

class Modifiers {
    @Transient
    val transientField: String = ""

    @Volatile
    var volatileField: String = ""

    @Strictfp
    fun strictFp() {}

    @JvmOverloads
    fun overloads(a: String = "", n: Int = 5): String = null!!
}
