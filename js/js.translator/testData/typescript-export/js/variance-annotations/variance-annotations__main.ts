import Covariant = JS_TESTS.foo.Covariant;
import Base = JS_TESTS.foo.Base;
import Consumer = JS_TESTS.foo.Consumer;
import Contravariant = JS_TESTS.foo.Contravariant;
import Invariant = JS_TESTS.foo.Invariant;
import InvariantBound = JS_TESTS.foo.InvariantBound;
import Producer = JS_TESTS.foo.Producer;
import Token = JS_TESTS.foo.Token;
import TokenBox = JS_TESTS.foo.TokenBox;
import UnsafeCovariant = JS_TESTS.foo.UnsafeCovariant;

class Animal {
    sayName() {}
}
class Dog extends Animal {
    bark() {}
}

class CustomToken extends Token {
    constructor() {
        super("custom");
    }
}

type ProducedValue = Producer<Base>["value"];
type ConsumerFunction = Consumer<Base>["consume"];

function acceptProducer(producer: Producer<Base>): ProducedValue {
    return producer.value;
}

function acceptConsumer(consumer: Consumer<Base>): ConsumerFunction {
    return consumer.consume;
}

function box(): string {
    const c1 = new Covariant<string>("123");
    const c2 = new Contravariant<string>();
    c2.consume("123");
    const c3 = new Invariant<number>(123);
    const c4: UnsafeCovariant<Animal> = new UnsafeCovariant<Dog>(new Dog());
    c4.consume(new Animal());

    const base = new Base("base");
    const producer = new Producer<Base>(base);
    const invariant = new InvariantBound<Base>(base);
    const consumer = { consume(_: Base) {} } as Consumer<Base>;

    acceptProducer(producer);
    acceptConsumer(consumer);
    invariant.value = base;

    const box = new TokenBox<CustomToken>(new CustomToken());
    const entry: TokenBox.Entry<CustomToken> = new box.Entry();
    const token: Token = entry.getValue();

    if (token.text !== "custom") {
        return "fail";
    }

    return "OK";
}
