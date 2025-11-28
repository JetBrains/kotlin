import Box = JS_TESTS.Box;
import GenericTestInner = JS_TESTS.GenericTestInner;

class StringBox<S extends string> implements Box<S, StringBox<S>> {
    private readonly v: S
    constructor(v: S) {
        this.v = v;
    }
    unbox(): S {
        return this.v;
    }

    copy(newValue: S): StringBox<S> {
        return new StringBox(newValue);
    }
}

function assertEquals<T>(expected: T, actual: T) {
    if (expected !== actual) {
        throw `Assertion failed: ${expected} is not equal to ${actual}`;
    }
}

function box(): string {
    const test = new GenericTestInner(new StringBox<string>("Hello "));

    const inner1 = new test.Inner(new StringBox("Inner"));
    assertEquals(inner1.concat, "Hello Inner");

    const inner2 = test.Inner.fromNumber(1);
    assertEquals(inner2.concat, "Hello 1");

    const innerInner = new inner1.SecondLayerInner(new StringBox("SecondLayer"));
    assertEquals(innerInner.concat, "Hello InnerSecondLayer");

    const genericInner1: GenericTestInner.GenericInner<StringBox<"GenericInner">, string, StringBox<string>> =
        new test.GenericInner(new StringBox("GenericInner"));
    assertEquals(genericInner1.concat, "Hello GenericInner");

    const genericInner2: GenericTestInner.GenericInner<StringBox<"GenericInner">, string, StringBox<string>> =
        test.GenericInner.fromNumber(2, genericInner1.a);
    assertEquals(genericInner2.concat, "Hello 2");

    const genericInnerInner: GenericTestInner.GenericInner.SecondLayerGenericInner<StringBox<"SecondLayer">, never[], StringBox<"GenericInner">, string, StringBox<string>> =
        new genericInner1.SecondLayerGenericInner(new StringBox("SecondLayer"), []);
    assertEquals(genericInnerInner.concat, "Hello GenericInnerSecondLayer");

    const genericInnerWithShadowing: GenericTestInner.GenericInnerWithShadowingTP<StringBox<"GenericInnerWithShadowing">, StringBox<string>> =
        new test.GenericInnerWithShadowingTP(new StringBox("GenericInnerWithShadowing"));
    assertEquals(genericInnerWithShadowing.concat, "Hello GenericInnerWithShadowing");

    const subOfInner: GenericTestInner.OpenInnerWithPublicConstructor<StringBox<string>> =
        new test.SubclassOfOpenInnerWithPublicConstructor(new StringBox("SubOfInner"));
    assertEquals(subOfInner.concat, "Hello SubOfInner");

    const subOfGenericInner: GenericTestInner.GenericOpenInnerWithPublicConstructor<StringBox<string>, StringBox<string>> =
        new test.SubclassOfGenericOpenInnerWithPublicConstructor(new StringBox("SubOfGenericInner"));
    assertEquals(subOfGenericInner.concat, "Hello SubOfGenericInner");

    const genericSubOfGenericInner: GenericTestInner.GenericSubclassOfGenericOpenInnerWithPublicConstructor1<StringBox<"GenericSubOfGenericInner">, StringBox<string>> =
        new test.GenericSubclassOfGenericOpenInnerWithPublicConstructor1(new StringBox("GenericSubOfGenericInner"));
    assertEquals(genericSubOfGenericInner.concat, "Hello GenericSubOfGenericInner");

    return "OK";
}