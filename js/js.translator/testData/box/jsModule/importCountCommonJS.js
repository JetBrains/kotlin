__beginModule__();
module.exports = function(param) {
    switch (typeof param) {
        case "number":
            return "a";
        case "string":
            return "b";
        default:
            return "c";
    }
};
__endModule__("lib");