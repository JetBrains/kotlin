export function bar() {
    return "(" + Array.prototype.join.call(arguments, "") + ")";
};
