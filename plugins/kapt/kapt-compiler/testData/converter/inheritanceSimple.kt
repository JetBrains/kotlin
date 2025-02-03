// STRIP_METADATA
// TODO: Re-enable metadata generation

interface Context

enum class Result {
    SUCCESS, ERROR
}

abstract class BaseClass(context: Context, num: Int, bool: Boolean) {
    abstract fun doJob(): Result
}

class Inheritor(context: Context) : BaseClass(context, 5, true) {
    override fun doJob() = Result.SUCCESS
}
