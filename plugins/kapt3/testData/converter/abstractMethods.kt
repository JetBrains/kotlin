abstract class Base {
    protected abstract fun doJob(job: String, delay: Int)
    protected abstract fun <T : CharSequence> doJobGeneric(job: T, delay: Int)
}

class Impl : Base() {
    override fun doJob(job: String, delay: Int) {}
    override fun <T : CharSequence> doJobGeneric(job: T, delay: Int) {}
}