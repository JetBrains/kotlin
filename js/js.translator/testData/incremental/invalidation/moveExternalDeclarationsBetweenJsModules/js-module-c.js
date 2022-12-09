(function (_) {
  'use strict';
  function externalDemoFunction() { return 4; }

  _.externalDemoFunction = externalDemoFunction;

  return _
}(module.exports));
