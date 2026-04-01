import foo = JS_TESTS.foo;

function assert(condition: boolean, message?: string) {
    if (!condition) {
        throw "Assertion failed" + (message ? `: ${message}` : "");
    }
}

function box(): string {
    // Basic value class creation and property access
    const intVal = new foo.IntValueClass(42);
    assert(intVal.value === 42, "IntValueClass value");

    const strVal = new foo.StringValueClass("hello");
    assert(strVal.name === "hello", "StringValueClass name");

    const doubleVal = new foo.DoubleValueClass(3.14);
    assert(doubleVal.amount === 3.14, "DoubleValueClass amount");

    const boolVal = new foo.BooleanValueClass(true);
    assert(boolVal.flag === true, "BooleanValueClass flag");

    // Nullable value class
    const nullableVal = new foo.NullableValueClass("data");
    assert(nullableVal.data === "data", "NullableValueClass with data");

    const nullableNull = new foo.NullableValueClass(null);
    assert(nullableNull.data === null, "NullableValueClass with null");

    // Generic value class
    const genericInt = new foo.GenericValueClass(100);
    assert(genericInt.item === 100, "GenericValueClass with Int");

    const genericString = new foo.GenericValueClass("test");
    assert(genericString.item === "test", "GenericValueClass with String");

    // Value class with methods
    const withMethods = new foo.ValueClassWithMethods(5);
    assert(withMethods.number === 5, "ValueClassWithMethods number");
    assert(withMethods.double() === 10, "ValueClassWithMethods.double()");
    assert(withMethods.add(3) === 8, "ValueClassWithMethods.add()");

    // Value class with companion object
    const withCompanion = new foo.ValueClassWithCompanion("test");
    assert(withCompanion.value === "test", "ValueClassWithCompanion value");
    assert(foo.ValueClassWithCompanion.Companion.DEFAULT === "default", "ValueClassWithCompanion.DEFAULT");
    
    const created = foo.ValueClassWithCompanion.Companion.create("created");
    assert(created.value === "created", "ValueClassWithCompanion.create()");

    // Functions accepting value classes
    const acceptResult = foo.acceptValueClass(intVal);
    assert(acceptResult === 42, "acceptValueClass");

    const createResult = foo.createValueClass(100);
    assert(createResult.value === 100, "createValueClass");

    const combineResult = foo.combineValueClasses(
        new foo.IntValueClass(10),
        new foo.IntValueClass(20)
    );
    assert(combineResult === 30, "combineValueClasses");

    // Generic function with value class
    const genericResult = foo.useGenericValueClass(genericString);
    assert(genericResult === "test", "useGenericValueClass");

    // Nested value class
    const nested = new foo.NestedValueClass(new foo.IntValueClass(99));
    assert(nested.inner.value === 99, "NestedValueClass");

    // Regular class with value class property
    const classWithValue = new foo.ClassWithValueProperty(strVal);
    assert(classWithValue.data.name === "hello", "ClassWithValueProperty");

    // Regular class with value class methods
    const classWithMethods = new foo.ClassWithValueMethods();
    const produced = classWithMethods.produceValue();
    assert(produced.value === 42, "ClassWithValueMethods.produceValue()");
    
    const consumed = classWithMethods.consumeValue(new foo.IntValueClass(7));
    assert(consumed === 7, "ClassWithValueMethods.consumeValue()");

    // Array of value classes
    const valueArray = foo.createValueArray();
    assert(valueArray.length === 3, "createValueArray length");
    assert(valueArray[0].value === 1, "createValueArray[0]");
    assert(valueArray[1].value === 2, "createValueArray[1]");
    assert(valueArray[2].value === 3, "createValueArray[2]");

    // Nullable value class parameter
    const nullableResult = foo.acceptNullableValueClass(new foo.IntValueClass(50));
    assert(nullableResult === 50, "acceptNullableValueClass with value");

    const nullableNullResult = foo.acceptNullableValueClass(null);
    assert(nullableNullResult === null, "acceptNullableValueClass with null");

    // Value class equality
    const eq1 = new foo.IntValueClass(42);
    const eq2 = new foo.IntValueClass(42);
    const eq3 = new foo.IntValueClass(43);
    assert(foo.compareValueClasses(eq1, eq2) === true, "compareValueClasses equal");
    assert(foo.compareValueClasses(eq1, eq3) === false, "compareValueClasses not equal");

    // Value class with interface
    const withInterface = new foo.ValueClassWithInterface(77);
    assert(withInterface.value === 77, "ValueClassWithInterface");

    // Value class with constructors
    const withCtor1 = new foo.ValueClassWithConstructors("direct");
    assert(withCtor1.data === "direct", "ValueClassWithConstructors primary");

    // Collections of value classes
    const listResult = foo.acceptValueClassList(foo.createValueClassList());
    assert(listResult === 6, "acceptValueClassList (1+2+3)");

    const arrayResult = foo.acceptValueClassArray([
        new foo.IntValueClass(5),
        new foo.IntValueClass(10),
        new foo.IntValueClass(15)
    ]);
    assert(arrayResult === 30, "acceptValueClassArray");

    // Class with value collections
    const classWithCollections = new foo.ClassWithValueCollections();
    assert(classWithCollections.getListSize() === 1, "ClassWithValueCollections initial size");
    
    classWithCollections.addToList(new foo.IntValueClass(20));
    assert(classWithCollections.getListSize() === 2, "ClassWithValueCollections after add");
    
    assert(classWithCollections.array.length === 2, "ClassWithValueCollections array length");
    assert(classWithCollections.array[0].name === "a", "ClassWithValueCollections array[0]");
    assert(classWithCollections.array[1].name === "b", "ClassWithValueCollections array[1]");

    // Value class wrapping a collection
    const valueWithCollection = foo.createValueClassWithCollection();
    assert(valueWithCollection.items !== undefined, "ValueClassWithCollection items exist");

    // Pair and Triple with value classes
    const pairResult = foo.createPairWithValueClass();
    assert(pairResult.first.value === 42, "createPairWithValueClass first");
    assert(pairResult.second.name === "answer", "createPairWithValueClass second");

    const tripleResult = foo.createTripleWithValueClass();
    assert(tripleResult.first.value === 1, "createTripleWithValueClass first");
    assert(tripleResult.second.name === "test", "createTripleWithValueClass second");
    assert(tripleResult.third.flag === true, "createTripleWithValueClass third");

    return "OK";
}