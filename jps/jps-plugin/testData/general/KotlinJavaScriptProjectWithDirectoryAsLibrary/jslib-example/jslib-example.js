if (typeof kotlin === 'undefined') {
  throw new Error("Error loading module 'LibraryExample'. Its dependency 'kotlin' was not found. Please, check whether 'kotlin' is loaded prior to 'LibraryExample'.");
}
var LibraryExample = function (_, Kotlin) {
  'use strict';
  function pairAdd(p) {
    return p.first + p.second | 0;
  }
  function pairMul(p) {
    return Kotlin.imul(p.first, p.second);
  }
  function IntHolder(value) {
    this.value = value;
  }
  IntHolder.$metadata$ = {
    kind: Kotlin.Kind.CLASS,
    simpleName: 'IntHolder',
    interfaces: []
  };
  IntHolder.prototype.component1 = function () {
    return this.value;
  };
  IntHolder.prototype.copy_za3lpa$ = function (value) {
    return new IntHolder(value === void 0 ? this.value : value);
  };
  IntHolder.prototype.toString = function () {
    return 'IntHolder(value=' + Kotlin.toString(this.value) + ')';
  };
  IntHolder.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.value) | 0;
    return result;
  };
  IntHolder.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.value, other.value))));
  };
  var package$library = _.library || (_.library = {});
  var package$sample = package$library.sample || (package$library.sample = {});
  package$sample.pairAdd_1fzo63$ = pairAdd;
  package$sample.pairMul_1fzo63$ = pairMul;
  package$sample.IntHolder = IntHolder;
  Kotlin.defineModule('LibraryExample', _);
  return _;
}(typeof LibraryExample === 'undefined' ? {} : LibraryExample, kotlin);
