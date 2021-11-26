export default function() {
    return "(" + Array.prototype.join.call(arguments, "") + ")";
};
