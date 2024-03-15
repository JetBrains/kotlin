@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.SinceKotlin(version = "2.0")
public inline fun <reified T : kotlin.Enum<T>> enumEntries(): kotlin.enums.EnumEntries<T>

@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public sealed interface EnumEntries<E : kotlin.Enum<E>> : kotlin.collections.List<E> {
}