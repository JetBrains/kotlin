var bar = {
    boo: function (a) {
        return "bar.boo(" + a + ")";
    },
    invoke: function () {
        return renderInvocation("bar.invoke", arguments)
    },
    num: 42,
    str: "ok!",
    obj: {
        name: "bar.obj"
    },
    1: {
        one: "1.one"
    },
    5: 0,
    0 : "zero",
    get: function(a, b, c) {
        return renderInvocation("bar.get", arguments)
    },
    set: function(a, b, c) {
        return renderInvocation("bar.set", arguments)
    }
};

var arr = [{ it : "is", 1 : "object" }, 65, undefined, -2, "Yo!"];

function baz() {
    return renderInvocation("baz", arguments)
}

function renderInvocation(prefix, args) {
    return prefix + "(" + Array.prototype.slice.call(args) + ")"
}