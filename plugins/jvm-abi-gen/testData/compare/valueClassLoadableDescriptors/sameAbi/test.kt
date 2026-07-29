// `V` is declared (so `V.class` is produced identically to `base`) but `Holder` has no value-class field, so no
// `LoadableDescriptors` attribute is emitted for it. Its ABI must match `base`'s `Holder`.
@JvmInline
value class V(val x: Int)

class Holder
