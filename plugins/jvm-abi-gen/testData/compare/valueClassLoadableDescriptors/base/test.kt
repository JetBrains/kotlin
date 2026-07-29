@JvmInline
value class V(val x: Int)

// The nullable value-class field is boxed to `LV;`, so under `-Xvalhalla-support` the compiler emits a `LoadableDescriptors`
// attribute on `Holder`. The field is private, so it is stripped from the ABI; the attribute is only a runtime preload hint (not
// part of the ABI), so jvm-abi-gen must drop it too. Hence this class has the same ABI as `sameAbi`'s `Holder`, which has no such
// field. If the attribute leaked into the ABI, the two would differ and this test would fail.
class Holder {
    private val v: V? = null
}
