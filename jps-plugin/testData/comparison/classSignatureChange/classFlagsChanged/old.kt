package test

class AbstractFlagAdded
abstract class AbstractFlagRemoved
abstract class AbstractFlagUnchanged

class AnnotationFlagAdded
annotation class AnnotationFlagRemoved
annotation class AnnotationFlagUnchanged

class DataFlagAdded(val x: Int)
data class DataFlagRemoved(val x: Int)
data class DataFlagUnchanged(val x: Int)

class EnumFlagAdded
enum class EnumFlagRemoved
enum class EnumFlagUnchanged

class FinalFlagAdded
final class FinalFlagRemoved
final class FinalFlagUnchanged

class InnerClassHolder {
    class InnerFlagAdded
    inner class InnerFlagRemoved
    inner class InnerFlagUnchanged
}

class InternalFlagAdded
internal class InternalFlagRemoved
internal class InternalFlagUnchanged

class OpenFlagAdded
open class OpenFlagRemoved
open class OpenFlagUnchanged

class PrivateFlagAdded
private class PrivateFlagRemoved
private class PrivateFlagUnchanged

class ProtectedClassHolder {
    class ProtectedFlagAdded
    protected class ProtectedFlagRemoved
    protected class ProtectedFlagUnchanged
}

class PublicFlagAdded
public class PublicFlagRemoved
public class PublicFlagUnchanged

class SealedFlagAdded
sealed class SealedFlagRemoved
sealed class SealedFlagUnchanged

class UnchangedNoFlags