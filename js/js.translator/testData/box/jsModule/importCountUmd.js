var f = function(param) {
    switch (typeof param) {
        case "number":
            return "a";
        case "string":
            return "b";
        default:
            return "c";
    }
};
var g = f;