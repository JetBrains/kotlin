"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (Object.prototype.hasOwnProperty.call(b, p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        if (typeof b !== "function" && b !== null)
            throw new TypeError("Class extends value " + String(b) + " is not a constructor or null");
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
var getI3 = JS_TESTS.foo.getI3;
var getA = JS_TESTS.foo.getA;
var getB = JS_TESTS.foo.getB;
var getC = JS_TESTS.foo.getC;
var B2 = JS_TESTS.foo.B2;
var C2 = JS_TESTS.foo.C2;
var EC = JS_TESTS.foo.EC;
var A2 = JS_TESTS.foo.A2;
var Impl = /** @class */ (function (_super) {
    __extends(Impl, _super);
    function Impl() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Impl.prototype.z = function (z) {
    };
    Object.defineProperty(Impl.prototype, "acAbstractProp", {
        get: function () { return "Impl"; },
        enumerable: false,
        configurable: true
    });
    Object.defineProperty(Impl.prototype, "y", {
        get: function () { return true; },
        enumerable: false,
        configurable: true
    });
    return Impl;
}(AC));
var A2Impl = /** @class */ (function (_super) {
    __extends(A2Impl, _super);
    function A2Impl() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this._bar = "barA2";
        return _this;
    }
    Object.defineProperty(A2Impl.prototype, "bar", {
        get: function () {
            return this._bar;
        },
        set: function (value) {
            this._bar = value;
        },
        enumerable: false,
        configurable: true
    });
    Object.defineProperty(A2Impl.prototype, "baz", {
        get: function () {
            return "bazA2";
        },
        enumerable: false,
        configurable: true
    });
    Object.defineProperty(A2Impl.prototype, "foo", {
        get: function () {
            return "fooA2";
        },
        enumerable: false,
        configurable: true
    });
    A2Impl.prototype.bay = function () {
        return "bayA2";
    };
    return A2Impl;
}(A2));
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
    if (getI3().foo != "fooI3")
        return "Fail 11";
    if (getI3().bar != "barI3")
        return "Fail 12";
    if (getI3().baz != "bazI3")
        return "Fail 13";
    if (getI3().bay() != "bayI3")
        return "Fail 14";
    if (getA().foo != "fooA")
        return "Fail 15";
    if (getA().bar != "barA")
        return "Fail 16";
    if (getA().baz != "bazA")
        return "Fail 17";
    if (getA().bay() != "bayA")
        return "Fail 18";
    if (getB().foo != "fooB")
        return "Fail 19";
    if (getB().bar != "barB")
        return "Fail 20";
    if (getB().baz != "bazB")
        return "Fail 21";
    if (getB().bay() != "bayB")
        return "Fail 22";
    if (getC().foo != "fooC")
        return "Fail 23";
    if (getC().bar != "barC")
        return "Fail 24";
    if (getC().baz != "bazC")
        return "Fail 25";
    if (getC().bay() != "bayC")
        return "Fail 26";
    var a2Impl = new A2Impl();
    if (a2Impl.foo != "fooA2")
        return "Fail 27";
    if (a2Impl.bar != "barA2")
        return "Fail 28";
    a2Impl.bar = "barA2.2";
    if (a2Impl.bar != "barA2.2")
        return "Fail 28.2";
    if (a2Impl.baz != "bazA2")
        return "Fail 29";
    if (a2Impl.bay() != "bayA2")
        return "Fail 30";
    var b2 = new B2();
    if (b2.foo != "fooB2")
        return "Fail 31";
    if (b2.bar != "barB2")
        return "Fail 32";
    if (b2.baz != "bazB2")
        return "Fail 33";
    if (b2.bay() != "bayB2")
        return "Fail 34";
    var c2 = new C2();
    if (c2.foo != "fooC2")
        return "Fail 35";
    if (c2.bar != "barC2")
        return "Fail 36";
    if (c2.baz != "bazC2")
        return "Fail 37";
    c2.baz = "bazC2-2";
    if (c2.baz != "bazC2-2")
        return "Fail 38";
    if (c2.bay() != "bayC2")
        return "Fail 39";
    if (EC.EC1.foo != "foo")
        return "Fail 40";
    if (EC.EC1.bar != "bar")
        return "Fail 41";
    if (EC.EC1.baz != "ec1")
        return "Fail 42";
    if (EC.EC1.bay() != "bay")
        return "Fail 43";
    if (EC.EC2.foo != "foo")
        return "Fail 44";
    if (EC.EC2.bar != "bar")
        return "Fail 45";
    if (EC.EC2.baz != "ec2")
        return "Fail 46";
    if (EC.EC2.bay() != "bay")
        return "Fail 47";
    if (EC.EC3.foo != "foo")
        return "Fail 48";
    if (EC.EC3.bar != "bar")
        return "Fail 49";
    if (EC.EC3.baz != "ec3")
        return "Fail 50";
    if (EC.EC3.bay() != "bay")
        return "Fail 51";
    return "OK";
}
