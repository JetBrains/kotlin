type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function getResult(): not.exported.org.second.Result<string>;
declare namespace not.exported.org.second {
    class Result<T extends NonNullable<unknown>> extends not.exported.org.second.BaseResult.$metadata$.constructor<T> {
        constructor();
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace Result.$metadata$ {
        const constructor: abstract new <T extends NonNullable<unknown>>() => Result<T>;
    }
}
declare namespace not.exported.org.second {
    abstract class BaseResult<T extends NonNullable<unknown>> {
        constructor(foo: typeof not.exported.org.second.Foo);
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace BaseResult.$metadata$ {
        const constructor: abstract new <T extends NonNullable<unknown>>() => BaseResult<T>;
    }
}
declare namespace not.exported.org.second {
    abstract class Foo extends KtSingleton<Foo.$metadata$.constructor>() {
        private constructor();
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace Foo.$metadata$ {
        abstract class constructor implements not.exported.Baz<string> {
            get bar(): not.exported.Parent.OneMoreLayer.MentionedNested;
            get baz(): string;
            get oneMore(): not.exported.Parent.Companion.AnotherMentionedNested;
            private constructor();
        }
    }
}
declare namespace not.exported {
    interface Baz<T> extends not.exported.Bar {
        readonly baz?: T;
        readonly bar: not.exported.Parent.OneMoreLayer.MentionedNested;
        readonly oneMore: not.exported.Parent.Companion.AnotherMentionedNested;
    }
}
declare namespace not.exported.Parent.OneMoreLayer {
    interface MentionedNested {
        readonly value: typeof not.exported.MentionedParent;
    }
}
declare namespace not.exported.Parent.Companion {
    class AnotherMentionedNested {
        constructor();
        get value(): string;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace AnotherMentionedNested.$metadata$ {
        const constructor: abstract new () => AnotherMentionedNested;
    }
}
declare namespace not.exported {
    interface Bar {
        readonly bar: not.exported.Parent.OneMoreLayer.MentionedNested;
        readonly oneMore: not.exported.Parent.Companion.AnotherMentionedNested;
    }
}
declare namespace not.exported {
    abstract class MentionedParent extends KtSingleton<MentionedParent.$metadata$.constructor>() {
        private constructor();
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace MentionedParent.$metadata$ {
        abstract class constructor {
            get value(): string;
            private constructor();
        }
    }
}
