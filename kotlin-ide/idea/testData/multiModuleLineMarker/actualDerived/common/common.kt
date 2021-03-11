
open class <!LINE_MARKER("descr='Is subclassed by ExpectedChild [common] ExpectedChild [jvm] SimpleChild'")!>SimpleParent<!>

expect class <!LINE_MARKER("descr='Has actuals in JVM'")!>ExpectedChild<!> : SimpleParent

class SimpleChild : SimpleParent()