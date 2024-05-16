import kotlin.native.internal.isPermanent

object Permanent

fun getPermanentId(permanent: Permanent) = permanent.hashCode()

fun idPermanent(permanent: Permanent) = permanent

fun getGlobalPermanent(): Permanent {
    check(Permanent.isPermanent())
    return Permanent
}