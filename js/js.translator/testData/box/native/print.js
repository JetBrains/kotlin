var buffer = "";

function writeToBuffer(a) {
    var type = typeof a;
    if (type === "undefined") return;
    if (type !== "string") throw Error("Expected string argument type, but got: " + type);

    buffer += a;
}

function writelnToBuffer(a) {
    writeToBuffer(a);
    writeToBuffer("\n");
}

var GLOBAL = (0, eval)("this");

GLOBAL.console = {
    log: writelnToBuffer
};

GLOBAL.outputStream =  {
    write: writeToBuffer
};

