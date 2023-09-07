import Foundation

@inline(__always)
func bridgeFromKotlin<T: AnyObject>(_ obj: UnsafeMutableRawPointer) -> T {
    let ptr = refToSwiftObject(obj)
    return Unmanaged<T>.fromOpaque(ptr).takeUnretainedValue()
}

@inline(__always)
func bridgeToKotlin<T: AnyObject>(_ obj: T, slot: UnsafeMutableRawPointer) -> UnsafeMutableRawPointer {
    let ptr = Unmanaged<T>.passUnretained(obj).toOpaque()
    return swiftObjectToRef(ptr, slot)
}

func withUnsafeTemporaryBufferAllocation<H, E, R>(
    ofHeader: H.Type = H.self,
    element: E.Type = E.self,
    count: Int,
    body: (UnsafeMutablePointer<H>, UnsafeMutableBufferPointer<E>) throws -> R
) rethrows -> R {
    assert(count >= 0)
    assert(MemoryLayout<E>.size > 0)

    let headerElementsCount = MemoryLayout<H>.size == 0 ? 0 : 1 + (MemoryLayout<H>.stride - 1) / MemoryLayout<E>.stride

    return try withUnsafeTemporaryAllocation(of: E.self, capacity: count + headerElementsCount) { buffer in
        try buffer.baseAddress!.withMemoryRebound(to: H.self, capacity: 1) { header in
            try body(header, .init(rebasing: buffer[headerElementsCount...]))
        }
    }
}

func withUnsafeSlots<R>(
    count: Int,
    body: (UnsafeMutableBufferPointer<UnsafeMutableRawPointer>) throws -> R
) rethrows -> R {
    guard count > 0 else { return try body(.init(start: nil, count: 0)) }
    return try withUnsafeTemporaryBufferAllocation(ofHeader: KObjHolderFrameInfo.self, element: UnsafeMutableRawPointer.self, count: count) { header, slots in
        header.initialize(to: .init(count: UInt32(count)))
        EnterFrame(header, 0, CInt(count))
        defer { LeaveFrame(header, 0, CInt(count))}
        return try body(slots)
    }
}

private struct KObjHolderFrameInfo {
    var arena: UnsafeMutableRawPointer? = nil
    var previous: UnsafeMutableRawPointer? = nil
    var parameters: UInt32 = 0
    var count: UInt32
}

extension UnsafeMutableBufferPointer {
    subscript(pointerAt offset: Int) -> UnsafeMutablePointer<Element> {
        return self.baseAddress!.advanced(by: offset)
    }
}