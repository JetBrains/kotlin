function paramCount() {
    return arguments.length
}

function Bar(size, order) {
    this.size = size;
    Bar.checkOrder(order);
}

Bar.order = 0;
Bar.hasOrderProblem = false;
Bar.checkOrder = function (expectedOrder) {
    var curOrder = Bar.order++;
    Bar.hasOrderProblem = Bar.hasOrderProblem || curOrder !== expectedOrder;
};

Bar.startNewTest = function () {
    Bar.hasOrderProblem = false;
    Bar.order = 0;
    return true;
};


Bar.prototype.test = function (order, dummy /*, args */) {
    Bar.checkOrder(order);
    return dummy === 1 && (arguments.length - 2) === this.size;
};
Bar.prototype.test2 = Bar.prototype.test;

function test3(bar, dummy  /*, args */) {
    return dummy === 1 && (arguments.length - 2) === bar.size;
}

var obj = {
    test: function (size /*, args */) {
        return (arguments.length - 1) === size;
    }
};

function testNativeVarargWithFunLit(/* args, f */) {
    var args = Array.prototype.slice.call(arguments, 0, arguments.length - 1);
    var f = arguments[arguments.length - 1];
    return typeof f === "function" && f(args);
}