function _enumerate(o) {
    var r = {};
    for (var p in o) {
        r[p] = o[p];
    }
    return r;
}

function _findFirst(o) {
    for (var p in o) {
        return o[p];
    }
}