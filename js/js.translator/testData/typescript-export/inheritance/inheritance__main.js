"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var OC = JS_TESTS.foo.OC;
var AC = JS_TESTS.foo.AC;
var FC = JS_TESTS.foo.FC;
var O1 = JS_TESTS.foo.O1;
var O2 = JS_TESTS.foo.O2;
var Impl = /** @class */ (function (_super) {
    __extends(Impl, _super);
    function Impl() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Impl.prototype.z = function (z) {
    };
    Object.defineProperty(Impl.prototype, "acAbstractProp", {
        get: function () { return "Impl"; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(Impl.prototype, "y", {
        get: function () { return true; },
        enumerable: true,
        configurable: true
    });
    return Impl;
}(AC));
function box() {
    var impl = new Impl();
    if (impl.acProp !== "acProp")
        return "Fail 1";
    if (impl.x !== "AC")
        return "Fail 2";
    if (impl.acAbstractProp !== "Impl")
        return "Fail 2.1";
    if (impl.y !== true)
        return "Fail 2.2";
    var oc = new OC(false, "OC");
    if (oc.y !== false)
        return "Fail 3";
    if (oc.acAbstractProp !== "OC")
        return "Fail 4";
    oc.z(10);
    var fc = new FC();
    if (fc.y !== true)
        return "Fail 5";
    if (fc.acAbstractProp !== "FC")
        return "Fail 6";
    fc.z(10);
    if (O1.y !== true || O2.y !== true)
        return "Fail 7";
    if (O1.acAbstractProp != "O1")
        return "Fail 8";
    if (O2.acAbstractProp != "O2")
        return "Fail 9";
    if (O2.foo() != 10)
        return "Fail 10";
    return "OK";
}
