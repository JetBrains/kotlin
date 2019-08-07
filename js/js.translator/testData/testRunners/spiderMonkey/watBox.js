// - Compile WAT file
// - Call exported function
// - Check the result

// Variables should be initialized before running this script
var file; // WAT file path
var fun;  // name of exported function to call
var res;  // expected result

if (typeof file != "string") throw `Invalid WAT file ${file}`;
if (typeof fun != "string") throw `Invalid function name ${fun}`;

const wat = read(file);
const wasmBinary = wasmTextToBinary(wat);
const wasmModule = new WebAssembly.Module(wasmBinary);
const wasmInstance = new WebAssembly.Instance(wasmModule);

const actualResult = wasmInstance.exports[fun]();
if (actualResult !== res)
    throw `Wrong box result '${actualResult}'; Expected '${res}'`;
