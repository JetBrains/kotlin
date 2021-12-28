function withMockedPrototype(constructor, functions) {
   class MockedConstructor extends constructor {}
   Object.assign(MockedConstructor.prototype, mocked(functions))
   return MockedConstructor
}

function withoutPropertiesInPrototype(constructor, properties) {
   return withMockedPrototype(constructor, emptyPropertiesFrom(properties))
}

function withMocks(obj, functions) {
   return withProperties(obj, mocked(functions));
}

function mocked(functions) {
   var mockedFunctions = {};
   for (var i in functions) {
      var func = functions[i];
      if (typeof func === "function") {
         mockedFunctions[i] = mock(func);
      } else {
         mockedFunctions[i] = func;
      }
   }
   return mockedFunctions;
}

function mock(func) {
  return function fn(...args) {
     fn.called = true;
     return func.apply(this, args);
  }
}

function withProperties(obj, properties) {
   var target = typeof obj === "function"
     ? class PropertyMock extends obj {}
     : Object.create(obj);

   return Object.assign(target, properties);
}

function withoutProperties(obj, properties) {
   return withProperties(obj, emptyPropertiesFrom(properties));
}

function emptyPropertiesFrom(properties) {
  return properties.reduce((a, b) => Object.assign(a, { [b]: undefined }), {});
}