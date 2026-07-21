// LANGUAGE: +CompanionExtensions

import kotlinx.atomicfu.*

public class PublicClass
internal class InternalClass
@PublishedApi internal class PublishedInternaClass
private class PrivateClass

<!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>public<!> companion val PublicClass.pa = atomic(0)
<!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> companion val PublicClass.pi = atomic(0)
@PublishedApi <!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> companion val PublicClass.pip = atomic(0)
private companion val PublicClass.pp = atomic(0)

<!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>public<!> companion val <!EXPOSED_RECEIVER_TYPE!>InternalClass<!>.pa = atomic(0)
<!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> companion val InternalClass.pi = atomic(0)
@PublishedApi <!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> companion val InternalClass.pip = atomic(0)
private companion val InternalClass.pp = atomic(0)

<!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>public<!> companion val <!EXPOSED_RECEIVER_TYPE!>PublishedInternaClass<!>.pa = atomic(0)
<!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> companion val PublishedInternaClass.pi = atomic(0)
@PublishedApi <!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> companion val PublishedInternaClass.pip = atomic(0)
private companion val PublishedInternaClass.pp = atomic(0)

<!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>public<!> companion val <!EXPOSED_RECEIVER_TYPE!>PrivateClass<!>.pa = atomic(0)
<!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> companion val <!EXPOSED_RECEIVER_TYPE!>PrivateClass<!>.pi = atomic(0)
@PublishedApi <!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> companion val <!EXPOSED_RECEIVER_TYPE!>PrivateClass<!>.pip = atomic(0)
private companion val PrivateClass.pp = atomic(0)

private companion <!ATOMIC_PROPERTIES_SHOULD_BE_VAL!>var<!> PrivateClass.volatileAtomic = atomic(0)

fun box(): String {
    return "OK"
}
