(function (_) {
  'use strict';
  function externalDemoFunction() { return 0; }

  _.externalDemoFunction = externalDemoFunction;

  return _
}(module.exports));
