(function (Kotlin) {
  'use strict';
  var _ = Kotlin.defineRootPackage(null, /** @lends _ */ {
    library: Kotlin.definePackage(null, /** @lends _.library */ {
      sample: Kotlin.definePackage(null, /** @lends _.library.sample */ {
        pairAdd_bunuun$: function (p) {
          return p.first + p.second;
        },
        pairMul_bunuun$: function (p) {
          return p.first * p.second;
        },
        IntHolder: Kotlin.createClass(null, function (value) {
          this.value = value;
        }, /** @lends _.library.sample.IntHolder.prototype */ {
          component1: function () {
            return this.value;
          },
          copy_za3lpa$: function (value) {
            return new _.library.sample.IntHolder(value === void 0 ? this.value : value);
          },
          toString: function () {
            return 'IntHolder(value=' + Kotlin.toString(this.value) + ')';
          },
          hashCode: function () {
            var result = 0;
            result = result * 31 + Kotlin.hashCode(this.value) | 0;
            return result;
          },
          equals_za3rmp$: function (other) {
            return this === other || (other !== null && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.value, other.value)));
          }
        })
      })
    })
  });
  Kotlin.defineModule('jslib-example', _);
}(Kotlin));
