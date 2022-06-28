declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        const _any: any;
        const _throwable: Error;
        const _string: string;
        const _boolean: boolean;
        const _byte: number;
        const _short: number;
        const _int: number;
        const _float: number;
        const _double: number;
        const _byte_array: Int8Array;
        const _short_array: Int16Array;
        const _int_array: Int32Array;
        const _float_array: Float32Array;
        const _double_array: Float64Array;
        const _array_byte: Array<number>;
        const _array_short: Array<number>;
        const _array_int: Array<number>;
        const _array_float: Array<number>;
        const _array_double: Array<number>;
        const _array_string: Array<string>;
        const _array_boolean: Array<boolean>;
        const _array_array_string: Array<Array<string>>;
        const _array_array_int_array: Array<Array<Int32Array>>;
        const _fun_unit: () => void;
        const _fun_int_unit: (p0: number) => void;
        const _fun_boolean_int_string_intarray: (p0: boolean, p1: number, p2: string) => Int32Array;
        const _curried_fun: (p0: number) => (p0: number) => (p0: number) => (p0: number) => (p0: number) => number;
        const _higher_order_fun: (p0: (p0: number) => string, p1: (p0: string) => number) => (p0: number) => number;
        const _n_any: Nullable<any>;
        const _n_nothing: Nullable<never>;
        const _n_throwable: Nullable<Error>;
        const _n_string: Nullable<string>;
        const _n_boolean: Nullable<boolean>;
        const _n_byte: Nullable<number>;
        const _n_short_array: Nullable<Int16Array>;
        const _n_array_int: Nullable<Array<number>>;
        const _array_n_int: Array<Nullable<number>>;
        const _n_array_n_int: Nullable<Array<Nullable<number>>>;
        const _array_n_array_string: Array<Nullable<Array<string>>>;
        const _fun_n_int_unit: (p0: Nullable<number>) => void;
        const _fun_n_boolean_n_int_n_string_n_intarray: (p0: Nullable<boolean>, p1: Nullable<number>, p2: Nullable<string>) => Nullable<Int32Array>;
        const _n_curried_fun: (p0: Nullable<number>) => (p0: Nullable<number>) => (p0: Nullable<number>) => Nullable<number>;
        const _n_higher_order_fun: (p0: (p0: Nullable<number>) => Nullable<string>, p1: (p0: Nullable<string>) => Nullable<number>) => (p0: Nullable<number>) => Nullable<number>;
        function _nothing(): never;
    }
}
