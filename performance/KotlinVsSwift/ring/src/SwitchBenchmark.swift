/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

struct ConstParams {
    static let SPARSE_SWITCH_CASES = [11, 29, 47, 71, 103,
                                     149, 175, 227, 263, 307,
                                     361, 487, 563, 617, 677,
                                     751, 823, 883, 967, 1031]

    static let V1 = 1
    static let V2 = 2
    static let V3 = 3
    static let V4 = 4
    static let V5 = 5
    static let V6 = 6
    static let V7 = 7
    static let V8 = 8
    static let V9 = 9
    static let V10 = 10
    static let V11 = 11
    static let V12 = 12
    static let V13 = 13
    static let V14 = 14
    static let V15 = 15
    static let V16 = 16
    static let V17 = 17
    static let V18 = 18
    static let V19 = 19
    static let V20 = 20
    static var VV1 = 1
    static var VV2 = 2
    static var VV3 = 3
    static var VV4 = 4
    static var VV5 = 5
    static var VV6 = 6
    static var VV7 = 7
    static var VV8 = 8
    static var VV9 = 9
    static var VV10 = 10
    static var VV11 = 11
    static var VV12 = 12
    static var VV13 = 13
    static var VV14 = 14
    static var VV15 = 15
    static var VV16 = 16
    static var VV17 = 17
    static var VV18 = 18
    static var VV19 = 19
    static var VV20 = 20
}


struct NumbersObj {
    static let shared = NumbersObj()

    private init() { }
    
    let V1 = 1
    let V2 = 2
    let V3 = 3
    let V4 = 4
    let V5 = 5
    let V6 = 6
    let V7 = 7
    let V8 = 8
    let V9 = 9
    let V10 = 10
    let V11 = 11
    let V12 = 12
    let V13 = 13
    let V14 = 14
    let V15 = 15
    let V16 = 16
    let V17 = 17
    let V18 = 18
    let V19 = 19
    let V20 = 20
}

class SwitchBenchmark {
    func sparseIntSwitch(_ u : Int) -> Int {
        var t : Int
        switch (u) {
        case 11:
            t = 1
        case 29:
            t = 2
        case 47:
            t = 3
        case 71:
            t = 4
        case 103:
            t = 5
        case 149:
            t = 6
        case 175:
            t = 7
        case 227:
            t = 1
        case 263:
            t = 9
        case 307:
            t = 1
        case 361:
            t = 2
        case 487:
            t = 3
        case 563:
            t = 4
        case 617:
            t = 4
        case 677:
            t = 4
        case 751:
            t = 435
        case 823:
            t = 31
        case 883:
            t = 1
        case 967:
            t = 1
        case 1031:
            t = 1
        case 20:
            t = 1
        default:
            t = 5
        }
        return t
    }

    func denseIntSwitch(_ u : Int) -> Int {
        var t : Int
        switch (u) {
        case 1:
            t = 1
        case -1:
            t = 2
        case 2:
            t = 3
        case 3:
            t = 4
        case 4:
            t = 5
        case 5:
            t = 6
        case 6:
            t = 7
        case 7:
            t = 1
        case 8:
            t = 9
        case 9:
            t = 1
        case 10:
            t = 2
        case 11:
            t = 3
        case 12:
            t = 4
        case 13:
            t = 4
        case 14:
            t = 4
        case 15:
            t = 435
        case 16:
            t = 31
        case 17:
            t = 1
        case 18:
            t = 1
        case 19:
            t = 1
        case 20:
            t = 1
        default:
            t = 5
        }
        return t
    }

    func constSwitch(_ u : Int) -> Int {
        var t : Int
        switch (u) {
        case ConstParams.V1:
            t = 1
        case ConstParams.V2:
            t = 3
        case ConstParams.V3:
            t = 4
        case ConstParams.V4:
            t = 5
        case ConstParams.V5:
            t = 6
        case ConstParams.V6:
            t = 7
        case ConstParams.V7:
            t = 1
        case ConstParams.V8:
            t = 9
        case ConstParams.V9:
            t = 1
        case ConstParams.V10:
            t = 2
        case ConstParams.V11:
            t = 3
        case ConstParams.V12:
            t = 4
        case ConstParams.V13:
            t = 4
        case ConstParams.V14:
            t = 4
        case ConstParams.V15:
            t = 435
        case ConstParams.V16:
            t = 31
        case ConstParams.V17:
            t = 1
        case ConstParams.V18:
            t = 1
        case ConstParams.V19:
            t = 1
        case ConstParams.V20:
            t = 1
        default:
            t = 5
        }
        return t
    }

    func objConstSwitch(_ u : Int) -> Int {
        var t : Int
        switch (u) {
        case NumbersObj.shared.V1:
            t = 1
        case NumbersObj.shared.V2:
            t = 3
        case NumbersObj.shared.V3:
            t = 4
        case NumbersObj.shared.V4:
            t = 5
        case NumbersObj.shared.V5:
            t = 6
        case NumbersObj.shared.V6:
            t = 7
        case NumbersObj.shared.V7:
            t = 1
        case NumbersObj.shared.V8:
            t = 9
        case NumbersObj.shared.V9:
            t = 1
        case NumbersObj.shared.V10:
            t = 2
        case NumbersObj.shared.V11:
            t = 3
        case NumbersObj.shared.V12:
            t = 4
        case NumbersObj.shared.V13:
            t = 4
        case NumbersObj.shared.V14:
            t = 4
        case NumbersObj.shared.V15:
            t = 435
        case NumbersObj.shared.V16:
            t = 31
        case NumbersObj.shared.V17:
            t = 1
        case NumbersObj.shared.V18:
            t = 1
        case NumbersObj.shared.V19:
            t = 1
        case NumbersObj.shared.V20:
            t = 1
        default:
            t = 5
        }
        return t
    }

    func varSwitch(_ u : Int) -> Int {
        var t : Int
        switch (u) {
        case ConstParams.VV1:
            t = 1
        case ConstParams.VV2:
            t = 3
        case ConstParams.VV3:
            t = 4
        case ConstParams.VV4:
            t = 5
        case ConstParams.VV5:
            t = 6
        case ConstParams.VV6:
            t = 7
        case ConstParams.VV7:
            t = 1
        case ConstParams.VV8:
            t = 9
        case ConstParams.VV9:
            t = 1
        case ConstParams.VV10:
            t = 2
        case ConstParams.VV11:
            t = 3
        case ConstParams.VV12:
            t = 4
        case ConstParams.VV13:
            t = 4
        case ConstParams.VV14:
            t = 4
        case ConstParams.VV15:
            t = 435
        case ConstParams.VV16:
            t = 31
        case ConstParams.VV17:
            t = 1
        case ConstParams.VV18:
            t = 1
        case ConstParams.VV19:
            t = 1
        case ConstParams.VV20:
            t = 1
        default:
            t = 5
        }
        return t
    }

    private func stringSwitch(_ s: String) -> Int {
        switch (s) {
        case "ABCDEFG1": return 1
        case "ABCDEFG2": return 2
        case "ABCDEFG2": return 3
        case "ABCDEFG3": return 4
        case "ABCDEFG4": return 5
        case "ABCDEFG5": return 6
        case "ABCDEFG6": return 7
        case "ABCDEFG7": return 8
        case "ABCDEFG8": return 9
        case "ABCDEFG9": return 10
        case "ABCDEFG10": return 11
        case "ABCDEFG11": return 12
        case "ABCDEFG12": return 1
        case "ABCDEFG13": return 2
        case "ABCDEFG14": return 3
        case "ABCDEFG15": return 4
        case "ABCDEFG16": return 5
        case "ABCDEFG17": return 6
        case "ABCDEFG18": return 7
        case "ABCDEFG19": return 8
        case "ABCDEFG20": return 9
        default: return -1
        }
    }

    var denseIntData: [Int]
    var sparseIntData: [Int]

    func testSparseIntSwitch() {
        for i in sparseIntData {
            Blackhole.consume(sparseIntSwitch(i))
        }
    }

    func testDenseIntSwitch() {
        for i in denseIntData {
            Blackhole.consume(denseIntSwitch(i))
        }
    }

    func testConstSwitch() {
        for i in denseIntData {
            Blackhole.consume(constSwitch(i))
        }
    }

    func testObjConstSwitch() {
        for i in denseIntData {
            Blackhole.consume(objConstSwitch(i))
        }
    }

    func testVarSwitch() {
        for i in denseIntData {
            Blackhole.consume(varSwitch(i))
        }
    }

    var data: [String] = []

    func testStringsSwitch() {
        let n = data.count
        for s in data {
            Blackhole.consume(stringSwitch(s))
        }
    }

    enum  MyEnum: CaseIterable {
        case ITEM1, ITEM2, ITEM3, ITEM4, ITEM5, ITEM6, ITEM7, ITEM8, ITEM9, ITEM10, ITEM11, ITEM12, ITEM13, ITEM14, ITEM15, ITEM16, ITEM17, ITEM18, ITEM19, ITEM20, ITEM21, ITEM22, ITEM23, ITEM24, ITEM25, ITEM26, ITEM27, ITEM28, ITEM29, ITEM30, ITEM31, ITEM32, ITEM33, ITEM34, ITEM35, ITEM36, ITEM37, ITEM38, ITEM39, ITEM40, ITEM41, ITEM42, ITEM43, ITEM44, ITEM45, ITEM46, ITEM47, ITEM48, ITEM49, ITEM50, ITEM51, ITEM52, ITEM53, ITEM54, ITEM55, ITEM56, ITEM57, ITEM58, ITEM59, ITEM60, ITEM61, ITEM62, ITEM63, ITEM64, ITEM65, ITEM66, ITEM67, ITEM68, ITEM69, ITEM70, ITEM71, ITEM72, ITEM73, ITEM74, ITEM75, ITEM76, ITEM77, ITEM78, ITEM79, ITEM80, ITEM81, ITEM82, ITEM83, ITEM84, ITEM85, ITEM86, ITEM87, ITEM88, ITEM89, ITEM90, ITEM91, ITEM92, ITEM93, ITEM94, ITEM95, ITEM96, ITEM97, ITEM98, ITEM99, ITEM100
    }

    private func enumSwitch(_ x: MyEnum) -> Int {
        switch (x) {
        case MyEnum.ITEM5: return 1
        case MyEnum.ITEM10: return 2
        case MyEnum.ITEM15: return 3
        case MyEnum.ITEM20: return 4
        case MyEnum.ITEM25: return 5
        case MyEnum.ITEM30: return 6
        case MyEnum.ITEM35: return 7
        case MyEnum.ITEM40: return 8
        case MyEnum.ITEM45: return 9
        case MyEnum.ITEM50: return 10
        case MyEnum.ITEM55: return 11
        case MyEnum.ITEM60: return 12
        case MyEnum.ITEM65: return 13
        case MyEnum.ITEM70: return 14
        case MyEnum.ITEM75: return 15
        case MyEnum.ITEM80: return 16
        case MyEnum.ITEM85: return 17
        case MyEnum.ITEM90: return 18
        case MyEnum.ITEM95: return 19
        case MyEnum.ITEM100: return 20
        default: return -1
        }
    }

    private func denseEnumSwitch(x: MyEnum) -> Int {
        switch (x) {
        case MyEnum.ITEM1: return 1
        case MyEnum.ITEM2: return 2
        case MyEnum.ITEM3: return 3
        case MyEnum.ITEM4: return 4
        case MyEnum.ITEM5: return 5
        case MyEnum.ITEM6: return 6
        case MyEnum.ITEM7: return 7
        case MyEnum.ITEM8: return 8
        case MyEnum.ITEM9: return 9
        case MyEnum.ITEM10: return 10
        case MyEnum.ITEM11: return 11
        case MyEnum.ITEM12: return 12
        case MyEnum.ITEM13: return 13
        case MyEnum.ITEM14: return 14
        case MyEnum.ITEM15: return 15
        case MyEnum.ITEM16: return 16
        case MyEnum.ITEM17: return 17
        case MyEnum.ITEM18: return 18
        case MyEnum.ITEM19: return 19
        case MyEnum.ITEM20: return 20
        default: return -1
        }
    }

    var enumData : [MyEnum]
    var denseEnumData : [MyEnum]

    func testEnumsSwitch() {
        let n = enumData.count - 1
        let data = enumData
        for i in 0...n {
            Blackhole.consume(enumSwitch(data[i]))
        }
    }

    func testDenseEnumsSwitch() {
        let n = denseEnumData.count - 1
        let data = denseEnumData
        for i in 0...n {
            Blackhole.consume(denseEnumSwitch(x: data[i]))
        }
    }

    class MySealedClass {
        class MySealedClass1: MySealedClass {}
        class MySealedClass2: MySealedClass {}
        class MySealedClass3: MySealedClass {}
        class MySealedClass4: MySealedClass {}
        class MySealedClass5: MySealedClass {}
        class MySealedClass6: MySealedClass {}
        class MySealedClass7: MySealedClass {}
        class MySealedClass8: MySealedClass {}
        class MySealedClass9: MySealedClass {}
        class MySealedClass10: MySealedClass {}
    }

    var sealedClassData: [MySealedClass]

    init() {
        data = []
        for _ in 0..<Constants.BENCHMARK_SIZE {
            data.append("ABCDEFG\(Int.random(in: 0...22))")
        }
        enumData = []
        for i in 0..<Constants.BENCHMARK_SIZE {
            enumData.append(MyEnum.allCases[i % MyEnum.allCases.count])
        }
        denseEnumData = []
        for i in 0..<Constants.BENCHMARK_SIZE {
            denseEnumData.append(MyEnum.allCases[i % 20])
        }
        denseIntData = []
        for _ in 0..<Constants.BENCHMARK_SIZE {
            denseIntData.append(Int.random(in: 0...25) - 1)
        }
        sparseIntData = []
        for _ in 0..<Constants.BENCHMARK_SIZE {
            sparseIntData.append(ConstParams.SPARSE_SWITCH_CASES[Int.random(in: 0..<20)])
        }
        sealedClassData = []
        for _ in 0..<Constants.BENCHMARK_SIZE {
            switch(Int.random(in: 0..<10)) {
            case 0: sealedClassData.append(MySealedClass.MySealedClass1())
            case 1: sealedClassData.append(MySealedClass.MySealedClass2())
            case 2: sealedClassData.append(MySealedClass.MySealedClass3())
            case 3: sealedClassData.append(MySealedClass.MySealedClass4())
            case 4: sealedClassData.append(MySealedClass.MySealedClass5())
            case 5: sealedClassData.append(MySealedClass.MySealedClass6())
            case 6: sealedClassData.append(MySealedClass.MySealedClass7())
            case 7: sealedClassData.append(MySealedClass.MySealedClass8())
            case 8: sealedClassData.append(MySealedClass.MySealedClass9())
            case 9: sealedClassData.append(MySealedClass.MySealedClass10())
            default: print("Exception.illigalStateException")
            }
        }
    }
    enum Exception: Error {
        case illigalStateException
    }

    private func sealedWhenSwitch(_ x: MySealedClass) -> Int {
        switch (x) {
        case is MySealedClass.MySealedClass1: return 1
        case is MySealedClass.MySealedClass2: return 2
        case is MySealedClass.MySealedClass3: return 3
        case is MySealedClass.MySealedClass4: return 4
        case is MySealedClass.MySealedClass5: return 5
        case is MySealedClass.MySealedClass6: return 6
        case is MySealedClass.MySealedClass7: return 7
        case is MySealedClass.MySealedClass8: return 8
        case is MySealedClass.MySealedClass9: return 9
        case is MySealedClass.MySealedClass10: return 10
        default:
            return -1
        }
    }

    func testSealedWhenSwitch() {
        let n = sealedClassData.count - 1
        for i in 0...n {
            Blackhole.consume(sealedWhenSwitch(sealedClassData[i]))
        }
    }
}
