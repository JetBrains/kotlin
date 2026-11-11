import DeepGenerics
import Foundation
import KotlinRuntime
import KotlinRuntimeSupport
import Testing

private final class SwiftIntRepo: KotlinBase, Repo {
    var stored: Int32
    init(_ initial: Int32) {
        self.stored = initial
        super.init()
    }
    func load() -> Int32 { stored }
    func store(item: Int32) -> Bool {
        stored = item
        return true
    }
}

private final class SwiftSubIntCodec: KotlinBase, sub.pkg.SubCodec {
    let offset: Int32
    init(offset: Int32) {
        self.offset = offset
        super.init()
    }
    func encode(value: Int32) -> String {
        "n:\(value + offset)"
    }
    func decode(raw: String) -> Int32 {
        let stripped = raw.replacingOccurrences(of: "n:", with: "")
        return (Int32(stripped) ?? 0) - offset
    }
}

private func assertFullCarrierMatrix<T: _KotlinBridgeable & Hashable>(
    sampleA: T,
    sampleB: T,
    expectedTagA: String,
    expectedTagB: String
) {
    // 1. Box<T>: init, getter, setter, snapshot, Kotlin runtime tag
    let box = Box(value: sampleA)
    #expect(box.value == sampleA)
    #expect(box.snapshot() == sampleA)
    #expect(kotlinRuntimeTag(x: box.value) == expectedTagA)
    box.value = sampleB
    #expect(box.value == sampleB)
    #expect(kotlinRuntimeTag(x: box.value) == expectedTagB)

    // 2. [T] (Array / List<T>)
    let list = echoList(items: [sampleA, sampleB])
    #expect(list == [sampleA, sampleB])
    #expect(reverseList(items: [sampleA, sampleB]) == [sampleB, sampleA])

    // 3. Set<T>
    let expectedSet: Set<T> = [sampleA, sampleB]
    #expect(echoSet(items: expectedSet) == expectedSet)

    // 4. [K: V] (Dictionary / Map<K, V>) as both key and value
    let dictByStr: [String: T] = ["a": sampleA, "b": sampleB]
    #expect(echoMap(map: dictByStr) == dictByStr)
    if sampleA != sampleB {
        let inverted = invertMap(map: dictByStr)
        #expect(inverted[sampleA] == "a")
        #expect(inverted[sampleB] == "b")
    }

    // 5. Optional<T> (.none & .some) in functions, Box<T?>, and [T?]
    let noneVal: T? = nil
    let someA: T? = sampleA
    #expect(echoNullable(x: noneVal) == nil)
    #expect(echoNullable(x: someA) == sampleA)
    let optBox = Box<T?>(value: nil)
    #expect(optBox.value == nil)
    optBox.value = sampleB
    #expect(optBox.value == sampleB)
    #expect(echoList(items: [someA, noneVal, sampleB]) == [sampleA, nil, sampleB])

    // 6. Generic functions (identity & PairBox swap)
    #expect(identity(x: sampleA) == sampleA)
    let swapped = swapPairBox(p: PairBox(first: sampleA, second: sampleB))
    #expect(swapped.first == sampleB)
    #expect(swapped.second == sampleA)
}

@Test
func fullPrimitiveAndValueTypeMatrixThroughGenerics() throws {
    // Signed integers (min / max / zero)
    assertFullCarrierMatrix(
        sampleA: Int8.min,
        sampleB: Int8.max,
        expectedTagA: "Byte:\(Int8.min)",
        expectedTagB: "Byte:\(Int8.max)"
    )
    #expect(kotlinRuntimeTag(x: Int8(0)) == "Byte:0")

    assertFullCarrierMatrix(
        sampleA: Int16.min,
        sampleB: Int16.max,
        expectedTagA: "Short:\(Int16.min)",
        expectedTagB: "Short:\(Int16.max)"
    )
    #expect(kotlinRuntimeTag(x: Int16(0)) == "Short:0")

    assertFullCarrierMatrix(
        sampleA: Int32.min,
        sampleB: Int32.max,
        expectedTagA: "Int:\(Int32.min)",
        expectedTagB: "Int:\(Int32.max)"
    )
    #expect(kotlinRuntimeTag(x: Int32(0)) == "Int:0")

    assertFullCarrierMatrix(
        sampleA: Int64.min,
        sampleB: Int64.max,
        expectedTagA: "Long:\(Int64.min)",
        expectedTagB: "Long:\(Int64.max)"
    )
    #expect(kotlinRuntimeTag(x: Int64(0)) == "Long:0")

    // Unsigned integers (0 & max, including UInt64.max = 18446744073709551615)
    assertFullCarrierMatrix(
        sampleA: UInt8(0),
        sampleB: UInt8.max,
        expectedTagA: "UByte:0",
        expectedTagB: "UByte:255"
    )
    assertFullCarrierMatrix(
        sampleA: UInt16(0),
        sampleB: UInt16.max,
        expectedTagA: "UShort:0",
        expectedTagB: "UShort:65535"
    )
    assertFullCarrierMatrix(
        sampleA: UInt32(0),
        sampleB: UInt32.max,
        expectedTagA: "UInt:0",
        expectedTagB: "UInt:4294967295"
    )
    assertFullCarrierMatrix(
        sampleA: UInt64(0),
        sampleB: UInt64.max,
        expectedTagA: "ULong:0",
        expectedTagB: "ULong:18446744073709551615"
    )

    // Floating-point types
    assertFullCarrierMatrix(
        sampleA: Float(0.0),
        sampleB: Float(-3.5),
        expectedTagA: "Float:0.0",
        expectedTagB: "Float:-3.5"
    )
    assertFullCarrierMatrix(
        sampleA: Double(0.0),
        sampleB: Double(2.5),
        expectedTagA: "Double:0.0",
        expectedTagB: "Double:2.5"
    )

    // Boolean (verifying false vs true vs nil)
    assertFullCarrierMatrix(
        sampleA: false,
        sampleB: true,
        expectedTagA: "Boolean:false",
        expectedTagB: "Boolean:true"
    )

    // Char / UTF16.CodeUnit: ASCII ('A') + non-ASCII BMP ('Ω' = 0x03A9 = 937, 'λ' = 0x03BB = 955).
    // Note: Swift stdlib defines `public typealias UTF16.CodeUnit = UInt16`, so Swift-originated
    // unconstrained generic `UTF16.CodeUnit` boxes via `UInt16: _KotlinBridgeable` (`UShort`),
    // while Kotlin-originated `kotlin.Char` unboxes seamlessly to `UTF16.CodeUnit`.
    let asciiA: UTF16.CodeUnit = Array("A".utf16)[0]
    let omega: UTF16.CodeUnit = Array("Ω".utf16)[0]
    let lambda: UTF16.CodeUnit = Array("λ".utf16)[0]
    assertFullCarrierMatrix(
        sampleA: asciiA,
        sampleB: omega,
        expectedTagA: "UShort:65",
        expectedTagB: "UShort:937"
    )
    assertFullCarrierMatrix(
        sampleA: omega,
        sampleB: lambda,
        expectedTagA: "UShort:937",
        expectedTagB: "UShort:955"
    )
    #expect(describeKotlinChar(c: asciiA) == "Char:65:A")
    #expect(describeKotlinChar(c: omega) == "Char:937:Ω")
    #expect(describeKotlinChar(c: lambda) == "Char:955:λ")
    let ktOmegaBox: Box<UTF16.CodeUnit> = makeKotlinCharBox(c: omega)
    #expect(ktOmegaBox.value == omega)
    #expect(ktOmegaBox.snapshot() == omega)
    #expect(makeKotlinBmpChars() == [asciiA, omega, lambda])
    #expect(joinKotlinChars(chars: [asciiA, omega, lambda]) == "AΩλ")
    #expect(echoKotlinCharSet(chars: Set([asciiA, omega, lambda])) == Set([asciiA, omega, lambda]))
    let charMap = makeKotlinCharMap()
    #expect(charMap == ["ascii": asciiA, "omega": omega, "lambda": lambda])
    #expect(invertKotlinCharMap(map: charMap) == [asciiA: "ascii", omega: "omega", lambda: "lambda"])
    #expect(joinVarargChars(chars: asciiA, omega, lambda) == "AΩλ")

    // Unit / KotlinUnit
    assertFullCarrierMatrix(
        sampleA: KotlinUnit.shared,
        sampleB: KotlinUnit(),
        expectedTagA: "Unit",
        expectedTagB: "Unit"
    )

    // Kotlin enum class Priority
    assertFullCarrierMatrix(
        sampleA: Priority.LOW,
        sampleB: Priority.CRITICAL,
        expectedTagA: "Priority:LOW",
        expectedTagB: "Priority:CRITICAL"
    )

    // KotlinAny & AnyHashable across Box, List, Set, Map, Optional, and generic functions
    let ktAnyFromKotlin: Box<KotlinAny> = makeBoxedKotlinAny(seed: 7)
    #expect(ktAnyFromKotlin.value.value as? String == "any-7")

    let anyBox = Box(value: KotlinAny(UInt64.max))
    #expect(anyBox.value.value as? UInt64 == UInt64.max)
    #expect(kotlinRuntimeTag(x: anyBox.value) == "ULong:18446744073709551615")

    let ktCharAnyBox = makeBoxedCharAsAny(c: omega)
    #expect(inspectBoxedAny(b: ktCharAnyBox) == "Char:937:Ω")
    #expect(ktCharAnyBox.value.value as? UTF16.CodeUnit == omega)

    let ktUnitAnyBox = makeBoxedUnitAsAny()
    #expect(inspectBoxedAny(b: ktUnitAnyBox) == "Unit")
    #expect(ktUnitAnyBox.value.value is KotlinBase)
    anyBox.value = KotlinAny(KotlinUnit.shared)
    #expect(anyBox.value.value is KotlinBase)
    #expect(kotlinRuntimeTag(x: anyBox.value) == "Unit")

    let anyList = echoList(items: [
        KotlinAny(Int32(11)),
        KotlinAny("txt"),
        KotlinAny(Dog(name: "Rex", breed: "Collie")),
        KotlinAny(KotlinUnit.shared),
        KotlinAny(Priority.HIGH),
    ])
    #expect(anyList.count == 5)
    #expect(anyList[0].value as? Int32 == 11)
    #expect(anyList[1].value as? String == "txt")
    #expect((anyList[2].value as? Dog)?.breed == "Collie")
    #expect(kotlinRuntimeTag(x: anyList[3]) == "Unit")
    #expect(kotlinRuntimeTag(x: anyList[4]) == "Priority:HIGH")

    let optAnyNone: KotlinAny? = nil
    let optAnySome: KotlinAny? = KotlinAny(Int16(-12))
    #expect(echoNullable(x: optAnyNone) == nil)
    #expect(echoNullable(x: optAnySome)?.value as? Int16 == -12)

    let anyHashSet = echoSet(items: Set([AnyHashable(Int32(5)), AnyHashable("key")]))
    #expect(anyHashSet.contains(AnyHashable(Int32(5))))
    #expect(anyHashSet.contains(AnyHashable("key")))

    let anyMap = echoMap(map: [AnyHashable("u64"): KotlinAny(UInt64.max)])
    #expect(anyMap[AnyHashable("u64")]?.value as? UInt64 == UInt64.max)
}

@Test
func deeplyNestedGenericsAndNullabilityEdgeCases() throws {
    // 1. Box<Box<Int32>> & Box<Box<Box<String>>>
    let ktNested = makeNestedIntBox(v: 42)
    #expect(ktNested.value.value == 42)
    ktNested.value.value = 108
    #expect(identity(x: ktNested).value.value == 108)

    let swiftNested = Box(value: Box(value: Int32(-7)))
    #expect(identity(x: swiftNested).value.value == -7)
    swiftNested.value = Box(value: Int32(99))
    #expect(swiftNested.value.value == 99)

    let triple = makeTripleNestedBox(s: "core")
    #expect(triple.value.value.value == "core")
    triple.value.value.value = "mutated"
    #expect(identity(x: triple).value.value.value == "mutated")

    // 2. Box<[String?]>
    let strListBox = Box<[String?]>(value: ["alpha", nil, "", "λ"])
    let upperBox = echoNestedNullableStrings(b: strListBox)
    #expect(upperBox.value == ["ALPHA", nil, "", "Λ"])

    // 3. [Box<Int32?>]
    let boxesOfNullableInts: [Box<Int32?>] = [
        Box(value: 0),
        Box(value: nil),
        Box(value: 25),
        Box(value: -5),
    ]
    #expect(sumNonNullBoxes(boxes: boxesOfNullableInts) == 20)
    let echoedBoxes = echoList(items: boxesOfNullableInts)
    #expect(echoedBoxes[0].value == 0)
    #expect(echoedBoxes[1].value == nil)
    #expect(echoedBoxes[2].value == 25)
    #expect(echoedBoxes[3].value == -5)

    // 4. [String: Box<Dog>]
    let dogDir: [String: Box<Dog>] = [
        "k9": Box(value: Dog(name: "Rex", breed: "Malinois")),
        "pup": Box(value: Puppy(name: "Pip", breed: "Beagle", ageWeeks: 10)),
    ]
    #expect(lookupDogBreed(directory: dogDir, key: "k9") == "Malinois")
    #expect(lookupDogBreed(directory: dogDir, key: "pup") == "Beagle")
    #expect(lookupDogBreed(directory: dogDir, key: "missing") == nil)
    let echoedDir = echoMap(map: dogDir)
    #expect((echoedDir["pup"]?.value as? Puppy)?.ageWeeks == 10)

    // 5. NullableHolder<T>(var item: T?) with T = Int32 (nil vs 0 vs 42) and T = Int32?
    let intHolder = NullableHolder<Int32>(item: nil)
    #expect(intHolder.item == nil)
    #expect(intHolder.isItemNull() == true)

    #expect(intHolder.replace(next: 0) == nil)
    #expect(intHolder.item == 0)
    #expect(intHolder.isItemNull() == false)

    #expect(intHolder.replace(next: 42) == 0)
    #expect(intHolder.item == 42)
    intHolder.item = nil
    #expect(intHolder.item == nil)
    #expect(intHolder.isItemNull() == true)

    // T = Int32? (nested optional Int32??; Kotlin runtime flattens nullability to single level)
    let nestedOptHolder = NullableHolder<Int32?>(item: .none)
    #expect(nestedOptHolder.isItemNull() == true)
    #expect(nestedOptHolder.item == nil)

    nestedOptHolder.item = .some(nil)
    #expect(nestedOptHolder.isItemNull() == true)
    #expect(nestedOptHolder.item == nil)

    nestedOptHolder.item = .some(0)
    #expect(nestedOptHolder.isItemNull() == false)
    #expect(nestedOptHolder.item == .some(0))

    nestedOptHolder.item = .some(42)
    #expect(nestedOptHolder.item == .some(42))

    // 6. Nullable upper bounds <T : Animal?>
    let nonNullSlot = ShelterSlot<Dog>(occupant: Dog(name: "Buster", breed: "Boxer"))
    #expect(nonNullSlot.occupantLabel() == "Buster")
    #expect(describeOccupant(occupant: nonNullSlot.occupant) == "Dog:Buster")

    let nullableSlot = ShelterSlot<Puppy?>(occupant: nil)
    #expect(nullableSlot.occupantLabel() == "vacant")
    #expect(describeOccupant(occupant: nullableSlot.occupant) == "none")

    nullableSlot.occupant = Puppy(name: "Mochi", breed: "Shiba", ageWeeks: 8)
    #expect(nullableSlot.occupantLabel() == "Mochi")
    #expect(describeOccupant(occupant: nullableSlot.occupant) == "Puppy:Mochi")

    // 7. Box<AnyRepo<Int32>> with hidden Kotlin, exported Kotlin, and Swift PAT conformers
    let hiddenRepo: any Repo<Int32> = makeHiddenRepo(initial: 10)
    let boxedHidden: Box<AnyRepo<Int32>> = wrapRepoInBox(repo: hiddenRepo)
    #expect(boxedHidden.value.load() == 10)
    #expect(updateBoxedRepo(boxed: boxedHidden, next: 33) == 33)
    #expect(hiddenRepo.load() == 33)

    let swiftRepo = SwiftIntRepo(50)
    let boxedSwiftRepo = Box<AnyRepo<Int32>>(value: AnyRepo(
        __externalRCRefUnsafe: _cloneBridgeableRef(swiftRepo.__externalRCRef()),
        options: .asExistentialWrapper
    ))
    #expect(boxedSwiftRepo.value.load() == 50)
    #expect(updateBoxedRepo(boxed: boxedSwiftRepo, next: 77) == 77)
    #expect(swiftRepo.stored == 77)

    let boxedSwiftViaKt = wrapRepoInBox(repo: swiftRepo)
    #expect(updateBoxedRepo(boxed: boxedSwiftViaKt, next: 123) == 123)
    #expect(swiftRepo.stored == 123)

    // 8. Sub-package generic types (package sub.pkg)
    let subEnv = sub.pkg.makeSubEnvelope(payload: "hello-sub", code: 200)
    #expect(subEnv.payload == "hello-sub")
    #expect(subEnv.code == 200)
    let nextEnv = subEnv.repackage(next: "v2")
    #expect(nextEnv.payload == "v2")
    #expect(nextEnv.code == 201)

    let boxedSubEnv: Box<sub.pkg.SubEnvelope<String>> = sub.pkg.boxSubEnvelope(env: nextEnv)
    #expect(boxedSubEnv.value.payload == "v2")

    let envOfBox = sub.pkg.SubEnvelope(payload: Box(value: Int32(900)), code: 1)
    #expect(identity(x: envOfBox).payload.value == 900)

    let ktCodec = sub.pkg.SubStringCodec(prefix: "tag")
    #expect(sub.pkg.roundTripSubCodec(codec: ktCodec, value: "payload") == "payload")

    let swiftSubCodec = SwiftSubIntCodec(offset: 10)
    #expect(sub.pkg.roundTripSubCodec(codec: swiftSubCodec, value: Int32(42)) == 42)
}

@Test
@available(macOS 13.0, *)
func higherOrderGenericFunctionsCurriedClosuresThrowsAndVarargs() throws {
    // 1. Multi-arg generic closures (A, B) -> C in both directions
    let combined = combineValues(a: Int32(6), b: "items") { count, label in
        Box(value: "\(count)-\(label)")
    }
    #expect(combined.value == "6-items")

    let builder: (String, UInt64) -> PairBox<String, UInt64> = makePairBuilder()
    let built = builder("max", UInt64.max)
    #expect(built.first == "max")
    #expect(built.second == UInt64.max)

    // 2. Nullable generic closures ((T) -> R)? in both directions
    let mappedSome = mapOrDefault(value: Int32(9), fallback: "none", transform: { v in "v=\(v * 2)" })
    #expect(mappedSome == "v=18")
    let mappedNil = mapOrDefault(value: Int32(9), fallback: "fallback", transform: nil)
    #expect(mappedNil == "fallback")

    let optBoxerSome: ((String) -> Box<String>)? = makeOptionalBoxer(enabled: true)
    try #require(optBoxerSome != nil)
    #expect(optBoxerSome!("wrapped").value == "wrapped")
    let optBoxerNil: ((String) -> Box<String>)? = makeOptionalBoxer(enabled: false)
    #expect(optBoxerNil == nil)

    // 3. Nested / curried generic closures (T) -> ((U) -> PairBox<T, U>) and stdlib Pair<T, U>
    let ktCurried: (Int32) -> ((String) -> PairBox<Int32, String>) = makeCurriedPairBox()
    let step1 = ktCurried(21)
    let curriedRes1 = step1("twenty-one")
    #expect(curriedRes1.first == 21)
    #expect(curriedRes1.second == "twenty-one")

    let swiftCurriedRes = applyCurriedPairBox(t: Dog(name: "Bolt", breed: "Husky"), u: UInt16(500)) { dog in
        { num in PairBox(first: dog, second: num + 1) }
    }
    #expect(swiftCurriedRes.first.breed == "Husky")
    #expect(swiftCurriedRes.second == 501)

    let ktStdlibCurried: (String) -> ((Int64) -> ExportedKotlinPackages.kotlin.Pair<String, Int64>) = makeCurriedStdlibPair()
    let stdPair1 = ktStdlibCurried("k")(Int64(-42))
    #expect(stdPair1.first == "k")
    #expect(stdPair1.second == -42)

    let stdPair2 = applyCurriedStdlibPair(t: Priority.HIGH, u: "ok") { prio in
        { msg in ExportedKotlinPackages.kotlin.Pair(first: prio, second: "\(msg)!") }
    }
    #expect(stdPair2.first == .HIGH)
    #expect(stdPair2.second == "ok!")

    // Higher-order generic closure taking a generic closure (((T) -> U)) -> U
    let hoRes = applyHigherOrder(seed: Int32(7), mapper: { "\($0 * 3)" }) { fn in
        "res:" + fn(7)
    }
    #expect(hoRes == "res:21")

    let ktHoInvoker: (@escaping (Int32) -> String) -> String = makeHigherOrderInvoker(seed: 12)
    #expect(ktHoInvoker({ "n=\($0)" }) == "n=12")

    // Curried closure returning optional inner closure (T) -> (((U) -> PairBox<T, U>)?)
    let activeCurried: (Int32) -> (((String) -> PairBox<Int32, String>)?) = makeOptionalCurried(active: true)
    #expect(activeCurried(5)?("five").second == "five")
    let inactiveCurried: (Int32) -> (((String) -> PairBox<Int32, String>)?) = makeOptionalCurried(active: false)
    #expect(inactiveCurried(5) == nil)

    // Generic closure returning optional PAT existential (T) -> (any Repo<T>)?
    let activeRepoFactory: (Int32) -> (any Repo<Int32>)? = makeOptionalRepoFactory(active: true)
    let builtOptRepo = activeRepoFactory(77)
    #expect(builtOptRepo?.load() == 77)
    let inactiveRepoFactory: (Int32) -> (any Repo<Int32>)? = makeOptionalRepoFactory(active: false)
    #expect(inactiveRepoFactory(77) == nil)

    let fallbackPair = PairBox(first: Int32(0), second: "fallback")
    let appliedOptSome = applyOptionalCurried(t: Int32(3), u: "three", fallback: fallbackPair) { t in
        { u in PairBox(first: t * 10, second: u) }
    }
    #expect(appliedOptSome.first == 30)
    #expect(appliedOptSome.second == "three")

    let appliedOptNil = applyOptionalCurried(t: Int32(3), u: "three", fallback: fallbackPair) { _ in nil }
    #expect(appliedOptNil.first == 0)
    #expect(appliedOptNil.second == "fallback")

    // 4. Stateful Swift closure mutating captured vars across Kotlin loop
    var callCount = 0
    var seenTrace: [Int32] = []
    let folded = foldItems(items: [Int32(2), Int32(4), Int32(6)], initial: "sum=0") { acc, next in
        callCount += 1
        seenTrace.append(next)
        return "\(acc)+\(next)"
    }
    #expect(callCount == 3)
    #expect(seenTrace == [2, 4, 6])
    #expect(folded == "sum=0+2+4+6")

    // 5. @Throws generic functions: success + concrete exception propagation
    let validBox = try requireValidItem(item: Int32(100), errorCode: 400) { $0 > 0 }
    #expect(validBox.value == 100)

    do {
        _ = try requireValidItem(item: "bad", errorCode: 422) { $0.count >= 10 }
        Issue.record("Expected GenericValidationException to be thrown")
    } catch let err as GenericValidationException {
        #expect(err.code == 422)
        #expect(err.message == "Rejected: bad")
    }

    // 6. Generic vararg functions (0, 1, and multiple args across primitives, optionals, objects, generic classes)
    let emptyInts: [Int32] = packVarargs()
    #expect(emptyInts.isEmpty)
    #expect(firstAndLastVararg() as PairBox<Int32, Int32>? == nil)

    #expect(packVarargs(items: Int8(-8), Int8(0), Int8(8)) == [-8, 0, 8])
    #expect(packVarargs(items: Int16(-16), Int16(16)) == [-16, 16])
    #expect(packVarargs(items: Int32(10), Int32(20), Int32(30)) == [10, 20, 30])
    #expect(packVarargs(items: Int64.min, Int64.max) == [Int64.min, Int64.max])
    #expect(packVarargs(items: Float(1.5), Float(-2.5)) == [1.5, -2.5])
    #expect(packVarargs(items: Double(3.25), Double(6.5)) == [3.25, 6.5])
    #expect(packVarargs(items: true, false, true) == [true, false, true])
    #expect(packVarargs(items: "one", "two", "three") == ["one", "two", "three"])

    let nullableStrVarargs: [String?] = packVarargs(items: "first", nil, "last")
    #expect(nullableStrVarargs == ["first", nil, "last"])

    let dogA = Dog(name: "A", breed: "Pug")
    let dogB = Dog(name: "B", breed: "Corgi")
    let dogBounds = firstAndLastVararg(items: dogA, dogB)
    #expect(dogBounds?.first.name == "A")
    #expect(dogBounds?.second.breed == "Corgi")

    let boxedVarargs = packVarargs(items: Box(value: Int32(1)), Box(value: Int32(2)))
    #expect(boxedVarargs.map(\.value) == [1, 2])
}

@Test
func multiLevelSealedVarianceChainsBoundsTypealiasesAndZeroStableRefLeaks() throws {
    // 1. Multi-level nested generic sealed hierarchy Outcome<out T> + sealed class Failure
    let puppyVal: any Outcome<Puppy> = outcomeValue(data: Puppy(name: "Scout", breed: "Terrier", ageWeeks: 12))
    switch puppyVal.sealedType() {
    case .value(let v):
        #expect(v.value.data.ageWeeks == 12)
    case .failure:
        Issue.record("Expected Outcome.Value")
    }

    let timeoutOut: any Outcome<Puppy> = outcomeTimeout()
    switch timeoutOut.sealedType() {
    case .failure(.timeout(let t)):
        #expect(t.value === _Outcome_Failure.Timeout.shared)
        #expect(t.value.category == "timeout")
    default:
        Issue.record("Expected Outcome.Failure.Timeout")
    }

    let httpOut: any Outcome<Puppy> = outcomeHttpError(code: 503, reason: "Unavailable")
    switch httpOut.sealedType() {
    case .failure(.httpError(let h)):
        #expect(h.value.code == 503)
        #expect(h.value.reason == "Unavailable")
        #expect(h.value.category == "http")
    default:
        Issue.record("Expected Outcome.Failure.HttpError")
    }

    // 2. 3-level variance chains across Puppy -> Dog -> Animal
    let puppy = Puppy(name: "Sunny", breed: "Golden", ageWeeks: 9)

    // Covariant class chain: OutBox<Puppy> -> OutBox<Dog> -> OutBox<Animal>
    let outPuppy = OutBox(item: puppy)
    let outDog = OutBox<Dog>(outPuppy.covariant)
    let outAnimal = OutBox<Animal>(outDog.covariant)
    #expect(outDog.item.breed == "Golden")
    #expect(readAnimalFromOutBox(box: outAnimal) == "Sunny")

    // Contravariant class chain: InSink<Animal> -> InSink<Dog> -> InSink<Puppy>
    let sinkAnimal = InSink<Animal>(label: "sink")
    let sinkDog = InSink<Dog>(sinkAnimal.contravariant)
    let sinkPuppy = InSink<Puppy>(sinkDog.contravariant)
    #expect(sinkPuppy.consume(value: puppy) == "sink:Puppy(Sunny,Golden,9w)")

    // Covariant PAT wrapper chain: AnySource<Puppy> -> AnySource<Dog> -> AnySource<Animal>
    let fixedPuppySrc = FixedSource(item: puppy)
    let anyPuppySrc = AnySource<Puppy>(
        __externalRCRefUnsafe: _cloneBridgeableRef(fixedPuppySrc.__externalRCRef()),
        options: .asExistentialWrapper
    )
    let anyDogSrc = AnySource<Dog>(anyPuppySrc.covariant)
    let anyAnimalSrc = AnySource<Animal>(anyDogSrc.covariant)
    #expect(anyDogSrc.next().breed == "Golden")
    #expect(readAnimalFromSource(src: anyAnimalSrc) == "Sunny")

    // Contravariant PAT wrapper chain: AnySink<Animal> -> AnySink<Dog> -> AnySink<Puppy>
    let ktAnimalSink = AnimalSink(tag: "pat")
    let anyAnimalSink = AnySink<Animal>(
        __externalRCRefUnsafe: _cloneBridgeableRef(ktAnimalSink.__externalRCRef()),
        options: .asExistentialWrapper
    )
    let anyDogSink = AnySink<Dog>(anyAnimalSink.contravariant)
    let anyPuppySink = AnySink<Puppy>(anyDogSink.contravariant)
    #expect(feedPuppyToSink(sink: anyPuppySink, puppy: puppy) == "pat:Sunny")

    // Covariant sealed enum projection chain: Outcome_SealedType<Puppy> -> AnyOutcome<Dog> -> AnyOutcome<Animal>
    let dogOutcome = AnyOutcome<Dog>(puppyVal.sealedType().covariant)
    let animalOutcome = AnyOutcome<Animal>(dogOutcome.sealedType().covariant)
    #expect(inspectAnimalOutcome(outcome: animalOutcome) == "ok:Scout")
    let animalHttpOutcome = AnyOutcome<Animal>(httpOut.sealedType().covariant)
    #expect(inspectAnimalOutcome(outcome: animalHttpOutcome) == "err:http:503:Unavailable")

    // 3. Multi-bounds <T> where T : Animal, T : Named, T : MyComparable<T>
    let champA = ChampionPuppy(name: "Ace", breed: "BorderCollie", ageWeeks: 14, badge: "Gold", score: 98)
    let champB = ChampionPuppy(name: "Flash", breed: "Whippet", ageWeeks: 15, badge: "Silver", score: 91)
    let crowned = crownChampion(a: champA, b: champB)
    #expect(crowned === champA)
    let podium = Podium(first: champB, second: champA)
    #expect(podium.winnerSummary() == "Ace#Gold")

    // 4. Generic typealiases
    let boxedList: BoxedList<Int32> = makeBoxedList(items: [1, 2, 3])
    #expect(boxedList.value == [1, 2, 3])
    let keyed: KeyedBoxes<String, Puppy> = ["winner": Box(value: puppy)]
    #expect(unwrapKeyedBox(map: keyed, key: "winner")?.ageWeeks == 9)
    #expect(unwrapKeyedBox(map: keyed, key: "other") == nil)
    let mappedViaAlias = runGenericMapper(item: UInt64.max) { "u:\($0)" }
    #expect(mappedViaAlias == "u:18446744073709551615")

    // 5. Hoisted companion & nested class on 2-param PairBox<A, B>
    #expect(PairBox<Int32, String>.Companion.shared.companionTag() == "PairBoxCompanion")
    let createdViaCompanion = PairBox<Int32, String>.Companion.shared.of(x: Int32(7), y: "seven")
    #expect(createdViaCompanion.first == 7)
    #expect(createdViaCompanion.second == "seven")
    let stamp = PairBox<Int32, String>.Stamp(label: "release", version: 3)
    #expect(stamp.describe() == "release@3")

    // 6. Zero StableRef leaks across 500 iterations of deep generic carriers, currying, PATs, and sealed types
    let omega: UTF16.CodeUnit = Array("Ω".utf16)[0]
    _ = identity(x: UInt64.max)
    _ = kotlinRuntimeTag(x: omega)
    _ = applyCurriedPairBox(t: Int32(1), u: "w") { a in { b in PairBox(first: a, second: b) } }

    let beforeRefs = currentStableRefs()
    try #require(beforeRefs >= 0)

    try autoreleasepool {
        for i: Int32 in 0..<500 {
            let nested = Box(value: Box(value: i))
            #expect(nested.value.value == i)
            #expect(kotlinRuntimeTag(x: UInt64.max) == "ULong:18446744073709551615")
            #expect(describeKotlinChar(c: omega) == "Char:937:Ω")
            #expect(makeKotlinCharBox(c: omega).value == omega)
            #expect(kotlinRuntimeTag(x: Priority.HIGH) == "Priority:HIGH")

            let curriedPair = applyCurriedPairBox(t: i, u: "k\(i)") { a in
                { b in PairBox(first: a + 1, second: b) }
            }
            #expect(curriedPair.first == i + 1)

            let valid = try requireValidItem(item: i, errorCode: 1) { $0 >= 0 }
            #expect(valid.value == i)

            let out: any Outcome<Puppy> = (i % 2 == 0) ? outcomeValue(data: puppy) : outcomeTimeout()
            let upcast = AnyOutcome<Animal>(out.sealedType().covariant)
            #expect(!inspectAnimalOutcome(outcome: upcast).isEmpty)
        }
    }

    let afterRefs = currentStableRefs()
    #expect(afterRefs - beforeRefs == 0, "Leaked \(afterRefs - beforeRefs) StableRefs!")
}
