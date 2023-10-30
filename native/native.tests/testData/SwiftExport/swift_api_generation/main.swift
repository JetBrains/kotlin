import Foundation

public func f(a: Int32, b: Int32) -> kotlin.Object {
    initRuntimeIfNeeded()
    switchThreadStateToRunnable()
    let result: kotlin.Object = withUnsafeSlots(count: 1, body: {  slots in bridgeFromKotlin(__kn_f__WithArgTypes__kotlinInt_kotlinInt(a, b, slots[pointerAt: 0])) })
    switchThreadStateToNative()
    return result
}

public func f(a: Double, b: Double) -> kotlin.Object {
    initRuntimeIfNeeded()
    switchThreadStateToRunnable()
    let result: kotlin.Object = withUnsafeSlots(count: 1, body: {  slots in bridgeFromKotlin(__kn_f__WithArgTypes__kotlinDouble_kotlinDouble(a, b, slots[pointerAt: 0])) })
    switchThreadStateToNative()
    return result
}

