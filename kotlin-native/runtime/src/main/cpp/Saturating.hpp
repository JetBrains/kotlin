/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>
#include <functional>
#include <limits>

namespace kotlin {

namespace internal {

// Not using std::common_type, because it allows to convert between unsigned and signed with losing information, and
// is otherwise not intuitive. Examples:
// * std::common_type_t<int8_t, uint32_t> is uint32_t - negative numbers are lost.
// * std::common_type_t<int8_t, int16_t> is int32_t - int16_t is enough, surely.
// * std::common_type_t<uint8_t, uint16_t> is int32_t - why did it become signed?
template <typename T, typename U>
struct wider {
    static_assert(std::is_integral_v<T>, "T must be integral");
    static_assert(std::is_integral_v<U>, "U must be integral");
    static_assert(std::is_signed_v<T> == std::is_signed_v<U>, "T and U must have the same sign");

    using type = std::conditional_t<sizeof(T) >= sizeof(U), T, U>;
};

template <typename T, typename U>
using wider_t = typename wider<T, U>::type;

} // namespace internal

template <typename U, typename T>
constexpr U saturating_cast(T value) noexcept {
    static_assert(std::is_integral_v<T>, "T must be integral");
    static_assert(std::is_integral_v<U>, "U must be integral");

    if constexpr (std::is_signed_v<T> == std::is_signed_v<U>) {
        if constexpr (sizeof(U) >= sizeof(T)) {
            // When T and U are of the same sign, and U can accomodate T, it's safe to convert it.
            return value;
        }
        if (value > static_cast<T>(std::numeric_limits<U>::max())) {
            // T is a bigger type and holds a bigger value than U can hold.
            return std::numeric_limits<U>::max();
        }
        if (value < static_cast<T>(std::numeric_limits<U>::min())) {
            // T is a bigger type and holds a smaller value than U can hold.
            return std::numeric_limits<U>::min();
        }
        // T is a bigger type, but its value fits in U.
        return static_cast<U>(value);
    } else if constexpr (std::is_signed_v<U>) {
        static_assert(!std::is_signed_v<T>);

        if constexpr (sizeof(U) > sizeof(T)) {
            // U is signed but strictly bigger than T, it's safe to convert.
            return static_cast<U>(value);
        }

        if (value > static_cast<T>(std::numeric_limits<U>::max())) {
            // T is a bigger type or is the same size but unsigned vs signed, and holds a bigger value than U can hold.
            return std::numeric_limits<U>::max();
        }
        // T is unsigned, and U is signed, so T's min cannot be less than U's min.
        // T is bigger, but its value fits in U.
        return static_cast<U>(value);
    } else {
        static_assert(std::is_signed_v<T>);
        static_assert(!std::is_signed_v<U>);

        if (value < 0) {
            // U is unsigned, its min is 0.
            return 0;
        }
        if constexpr (sizeof(U) >= sizeof(T)) {
            // T is signed, U - unsigned, and U is not less than T. So, max of T is less than max of U.
            return static_cast<U>(value);
        }
        if (value > static_cast<T>(std::numeric_limits<U>::max())) {
            // T is a bigger type and holds a bigger value than U can hold.
            return std::numeric_limits<U>::max();
        }
        // T is a bigger type but its value fits in U.
        return static_cast<U>(value);
    }
}

template <typename T, typename U>
constexpr auto saturating_add(T lhs, U rhs) noexcept {
    static_assert(std::is_integral_v<T>, "T must be integral");
    static_assert(std::is_integral_v<U>, "U must be integral");
    static_assert(std::is_signed_v<T> == std::is_signed_v<U>, "T and U must have the same sign");

    using Result = internal::wider_t<T, U>;
    Result result;
    if (__builtin_add_overflow(lhs, rhs, &result)) {
        if (rhs >= 0) {
            // Adding a non-negative number caused an overflow => overflowed upwards.
            result = std::numeric_limits<Result>::max();
        } else {
            // Adding a negative number caused an overflow => overflowed downwards.
            result = std::numeric_limits<Result>::min();
        }
    }
    return result;
}

template <typename T, typename U>
constexpr auto saturating_sub(T lhs, U rhs) noexcept {
    static_assert(std::is_integral_v<T>, "T must be integral");
    static_assert(std::is_integral_v<U>, "U must be integral");
    static_assert(std::is_signed_v<T> == std::is_signed_v<U>, "T and U must have the same sign");

    using Result = internal::wider_t<T, U>;
    Result result;
    if (__builtin_sub_overflow(lhs, rhs, &result)) {
        if (rhs >= 0) {
            // Subtracting a non-negative number caused an overflow => overflowed downwards.
            result = std::numeric_limits<Result>::min();
        } else {
            // Subtracting a negative number caused an overflow => overflowed upwards.
            result = std::numeric_limits<Result>::max();
        }
    }
    return result;
}

template <typename T, typename U>
constexpr auto saturating_mul(T lhs, U rhs) noexcept {
    static_assert(std::is_integral_v<T>, "T must be integral");
    static_assert(std::is_integral_v<U>, "U must be integral");
    static_assert(std::is_signed_v<T> == std::is_signed_v<U>, "T and U must have the same sign");

    using Result = internal::wider_t<T, U>;
    Result result;
    if (__builtin_mul_overflow(lhs, rhs, &result)) {
        if (lhs >= 0 && rhs >= 0) {
            // Multiplying non-negative numbers caused an overflow => overflowed upwards.
            result = std::numeric_limits<Result>::max();
        } else if (lhs < 0 && rhs < 0) {
            // Multiplying negative numbers caused an overflow => overflowed upwards.
            result = std::numeric_limits<Result>::max();
        } else {
            // Multiplying non-negative number by a negative number caused an overflow => overflowed downwards.
            result = std::numeric_limits<Result>::min();
        }
    }
    return result;
}

template <typename T>
struct saturating {
    static_assert(std::is_integral_v<T>, "saturating is only defined for integers.");

    using value_type = T;

    // The underlying value.
    T value;

    // Construct from any value using saturating_cast.
    template <typename U>
    explicit constexpr saturating(U value) noexcept : value(saturating_cast<T>(value)) {}

    // Construct from any other saturating type using saturating_cast.
    template <typename U>
    explicit constexpr saturating(saturating<U> other) noexcept : value(saturating_cast<T>(other.value)) {}

    ~saturating() = default;
    constexpr saturating(const saturating&) noexcept = default;
    constexpr saturating& operator=(const saturating&) noexcept = default;

    // Explicitly convert into any value using saturating_cast.
    template <typename U>
    explicit constexpr operator U() const noexcept {
        return saturating_cast<U>(value);
    }

    constexpr bool operator==(saturating other) const noexcept { return value == other.value; }

    constexpr bool operator!=(saturating other) const noexcept { return value != other.value; }

    constexpr bool operator<(saturating other) const noexcept { return value < other.value; }

    constexpr bool operator<=(saturating other) const noexcept { return value <= other.value; }

    constexpr bool operator>(saturating other) const noexcept { return value > other.value; }

    constexpr bool operator>=(saturating other) const noexcept { return value >= other.value; }
};

template <typename T>
saturating(T) -> saturating<T>;

// Saturated addition.
template <typename T, typename U>
constexpr auto operator+(saturating<T> lhs, saturating<U> rhs) noexcept {
    return saturating(saturating_add(lhs.value, rhs.value));
}

// Saturated addition.
template <typename T, typename U>
constexpr saturating<T>& operator+=(saturating<T>& lhs, saturating<U> rhs) noexcept {
    auto result = lhs + rhs;
    lhs = saturating<T>(result);
    return lhs;
}

// Saturated subtraction.
template <typename T, typename U>
constexpr auto operator-(saturating<T> lhs, saturating<U> rhs) noexcept {
    return saturating(saturating_sub(lhs.value, rhs.value));
}

// Saturated subtraction.
template <typename T, typename U>
constexpr saturating<T>& operator-=(saturating<T>& lhs, saturating<U> rhs) noexcept {
    auto result = lhs - rhs;
    lhs = saturating<T>(result);
    return lhs;
}

// Saturated multiplication.
template <typename T, typename U>
constexpr auto operator*(saturating<T> lhs, saturating<U> rhs) noexcept {
    return saturating(saturating_mul(lhs.value, rhs.value));
}

// Saturated multiplication.
template <typename T, typename U>
constexpr saturating<T>& operator*=(saturating<T>& lhs, saturating<U> rhs) noexcept {
    auto result = lhs * rhs;
    lhs = saturating<T>(result);
    return lhs;
}

// TODO: Saturated division and modulo. (there are no builtins for that)
// TODO: Saturated negation: for signed types -MIN overflows MAX; but what to do for unsigned types?

using int_sat8_t = saturating<int8_t>;
using int_sat16_t = saturating<int16_t>;
using int_sat32_t = saturating<int32_t>;
using int_sat64_t = saturating<int64_t>;
using uint_sat8_t = saturating<uint8_t>;
using uint_sat16_t = saturating<uint16_t>;
using uint_sat32_t = saturating<uint32_t>;
using uint_sat64_t = saturating<uint64_t>;
using size_sat_t = saturating<size_t>;

} // namespace kotlin

namespace std {

template <typename T, typename U>
struct common_type<kotlin::saturating<T>, kotlin::saturating<U>> {
    static_assert(std::is_signed_v<T> == std::is_signed_v<U>, "Requires T and U be of same signedness");

    using type = kotlin::saturating<kotlin::internal::wider_t<T, U>>;
};

template <typename T, typename U>
struct common_type<kotlin::saturating<T>, U> {
    static_assert(std::is_integral_v<U>, "Requires U to be integral");
    static_assert(std::is_signed_v<T> == std::is_signed_v<U>, "Requires T and U be of same signedness");

    using type = kotlin::saturating<kotlin::internal::wider_t<T, U>>;
};

template <typename T>
struct hash<kotlin::saturating<T>> {
    size_t operator()(const kotlin::saturating<T>& value) const { return hash<T>(value.value); }
};

template <typename T>
class numeric_limits<kotlin::saturating<T>> {
public:
    static constexpr bool is_specialized = true;
    static constexpr bool is_signed = numeric_limits<T>::is_signed;
    static constexpr bool is_integer = numeric_limits<T>::is_integer;
    static constexpr bool is_exact = numeric_limits<T>::is_exact;
    static constexpr bool has_infinity = numeric_limits<T>::has_infinity;
    static constexpr bool has_quiet_NaN = numeric_limits<T>::has_quiet_NaN;
    static constexpr bool has_signaling_NaN = numeric_limits<T>::has_signaling_NaN;
    static constexpr std::float_denorm_style has_denorm = numeric_limits<T>::has_denorm;
    static constexpr bool has_denorm_loss = numeric_limits<T>::has_denorm_loss;
    static constexpr std::float_round_style round_style = numeric_limits<T>::round_style;
    static constexpr bool is_iec559 = numeric_limits<T>::is_iec559;
    static constexpr bool is_bounded = numeric_limits<T>::is_bounded;
    static constexpr bool is_modulo = false; // Because it's saturating.
    static constexpr int digits = numeric_limits<T>::digits;
    static constexpr int digits10 = numeric_limits<T>::digits10;
    static constexpr int max_digits10 = numeric_limits<T>::max_digits10;
    static constexpr int radix = numeric_limits<T>::radix;
    static constexpr int min_exponent = numeric_limits<T>::min_exponent;
    static constexpr int min_exponent10 = numeric_limits<T>::min_exponent10;
    static constexpr int max_exponent = numeric_limits<T>::max_exponent;
    static constexpr int max_exponent10 = numeric_limits<T>::max_exponent10;
    static constexpr bool traps = numeric_limits<T>::traps;
    static constexpr bool tinyness_before = numeric_limits<T>::tinyness_before;

    static constexpr kotlin::saturating<T> min() noexcept { return kotlin::saturating(std::numeric_limits<T>::min()); }
    static constexpr kotlin::saturating<T> lowest() noexcept { return kotlin::saturating(std::numeric_limits<T>::lowest()); }
    static constexpr kotlin::saturating<T> max() noexcept { return kotlin::saturating(std::numeric_limits<T>::max()); }
    static constexpr kotlin::saturating<T> epsilon() noexcept { return kotlin::saturating(std::numeric_limits<T>::epsilon()); }
    static constexpr kotlin::saturating<T> round_error() noexcept { return kotlin::saturating(std::numeric_limits<T>::round_error()); }
    static constexpr kotlin::saturating<T> infinity() noexcept { return kotlin::saturating(std::numeric_limits<T>::infinity()); }
    static constexpr kotlin::saturating<T> quiet_NaN() noexcept { return kotlin::saturating(std::numeric_limits<T>::quiet_NaN()); }
    static constexpr kotlin::saturating<T> signaling_NaN() noexcept { return kotlin::saturating(std::numeric_limits<T>::signaling_NaN()); }
    static constexpr kotlin::saturating<T> denorm_min() noexcept { return kotlin::saturating(std::numeric_limits<T>::denorm_min()); }
};

} // namespace std
