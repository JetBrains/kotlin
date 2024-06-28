module.exports = function (step) {
    const lib1 = require("./main.js")

    let result;
    switch (step) {
        case 0: return "OK"
        case 1:
            result = lib1.foo()
            return result === 77 ? "OK" : `Fail 1: Expect 77 but got ${result}`
        case 2:
            result = lib1.bar()
            return result === 77 ? "OK" : `Fail 2: Expect 77 but got ${result}`
        case 3:
            result = lib1.baz()
            return result === 77 ? "OK" : `Fail 3: Expect 77 but got ${result}`
        case 4:
            result = lib1.baz()
            if (result !== 77) return `Fail 4: Expect 77 but got ${result}`
            result = lib1.gaz()
            return result === 99 ? "OK" : `Fail 4: Expect 99 but got ${result}`
        case 5:
            result = lib1.baz()
            if (result !== 77) return `Fail 5: Expect 77 but got ${result}`
            result = lib1.gaz77()
            return result === 99 ? "OK" : `Fail 5: Expect 99 but got ${result}`
        case 6:
            result = lib1.baz()
            if (result !== 77) return `Fail 6: Expect 77 but got ${result}`
            result = lib1.gaz99()
            return result === 99 ? "OK" : `Fail 6: Expect 99 but got ${result}`
    }

    return "OK"
}