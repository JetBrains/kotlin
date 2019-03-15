// Note right now it works only for IR tests.
// With IDEA 2018.3 or later you can just activate required js and run "run IR test in node.js" configuration.

// Add to this array your path to test files or provide it as argument.
var anotherFiles = [""];

var vm = require('vm');
var fs = require('fs');

// Change working dir to root of project
var testDataPathFromRoot = "js/js.translator/testData";
var cwd = process.cwd();
if (cwd.endsWith(testDataPathFromRoot)) {
    process.chdir(cwd.substr(0, cwd.length - testDataPathFromRoot.length));
}

var filesFromArgs = process.argv.slice(2);

function toAbsolutePath(path) {
    if (fs.existsSync(path) && fs.statSync(path).isFile()) {
        return fs.realpathSync(path)
    }

    return "";
}

// TODO autodetect common js files and other js files
// Filter out all except existing js files and transform all paths to absolute
var files = [].concat(filesFromArgs, anotherFiles)
    .map(toAbsolutePath)
    .filter(function(path) {
        return path.endsWith(".js")
    });

// Find runtime path

var runtimeHeader = "// RUNTIME: ";
var runtimeFiles = [];
files.forEach(function (path) {
    var code = fs.readFileSync(path, 'utf8');
    var firstLine = code.substr(0, code.indexOf("\n"));
    if (firstLine.startsWith(runtimeHeader)) {
        runtimeFiles = JSON.parse(firstLine.slice(runtimeHeader.length))
            .map(toAbsolutePath);
    }
});

var allFiles = [].concat(runtimeFiles, files);

// Evaluate files and run box function

var sandbox = {};
vm.createContext(sandbox);

allFiles.forEach(function(path) {
    var code = fs.readFileSync(path, 'utf8');
    vm.runInContext(code, sandbox, {
        filename: path
    })
});

// TODO: Support multimodule tests
console.log(vm.runInContext("JS_TESTS.box()", sandbox));
