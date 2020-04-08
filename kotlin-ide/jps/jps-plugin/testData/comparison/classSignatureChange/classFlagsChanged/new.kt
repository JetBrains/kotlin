package test

abstract class AbstractFlagAdded
class AbstractFlagRemoved
abstract class AbstractFlagUnchanged

annotation class AnnotationFlagAdded
class AnnotationFlagRemoved
annotation class AnnotationFlagUnchanged

data class DataFlagAdded(val x: Int)
class DataFlagRemoved(val x: Int)
data class DataFlagUnchanged(val x: Int)

enum class EnumFlagAdded
class EnumFlagRemoved
enum class EnumFlagUnchanged

final class FinalFlagAdded
class FinalFlagRemoved
final class FinalFlagUnchanged

class InnerClassHolder {
    inner class InnerFlagAdded
    class InnerFlagRemoved
    inner class InnerFlagUnchanged
}

internal class InternalFlagAdded
class InternalFlagRemoved
internal class InternalFlagUnchanged

open class OpenFlagAdded
class OpenFlagRemoved
open class OpenFlagUnchanged

private class PrivateFlagAdded
class PrivateFlagRemoved
private class PrivateFlagUnchanged

class ProtectedClassHolder {
    protected class ProtectedFlagAdded
    class ProtectedFlagRemoved
    protected class ProtectedFlagUnchanged
}

public class PublicFlagAdded
class PublicFlagRemoved
public class PublicFlagUnchanged

sealed class SealedFlagAdded
class SealedFlagRemoved
sealed class SealedFlagUnchanged

class UnchangedNoFlags