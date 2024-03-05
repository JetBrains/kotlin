define("lib", [], function () {
    function createX () {
        return {x: "X1"};
    }
    function createXY () {
        return {x: "X2", y: "Y2"};
    }
    function createXYZ() {
        return {x: "X3", y: "Y3", z: "Z3"};
    }
    function createClassXYZ() {
        return {x: "X4", y: "Y4", z: "Z4"};
    }
    function createNestedInterfaceXYZ() {
        return {x: "X5", y: "Y5", z: "Z5"};
    }

    const x = "X6"
    const y = "Y6"
    const z = "Z6"

    return {
        createX,
        createXY,
        createXYZ,
        createClassXYZ,
        createNestedInterfaceXYZ,
        x,
        y,
        z
    }
})