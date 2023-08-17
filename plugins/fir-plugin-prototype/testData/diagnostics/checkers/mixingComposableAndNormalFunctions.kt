import org.jetbrains.kotlin.fir.plugin.MyComposable
import kotlin.reflect.*

abstract class MyClass: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction0<Unit>, () -> Unit<!>
abstract class OurClass: <!MIXING_FUNCTIONAL_KINDS_IN_SUPERTYPES!>@MyComposable (Int) -> Unit, () -> Unit<!>

abstract class YourClass: @MyComposable KFunction1<Boolean, Unit>, () -> Unit
abstract class Their: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>KSuspendFunction1<Boolean, Unit>, () -> Unit<!>
