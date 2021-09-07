/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Exceptions.h"

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "ObjectTestSupport.hpp"
#include "TestSupportCompilerGenerated.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

using ::testing::_;
using ::kotlin::test_support::ScopedReportUnhandledExceptionMock;
using ::kotlin::test_support::ScopedKotlin_runUnhandledExceptionHookMock;

namespace {

struct Payload {
    int value = 0;

    using Field = ObjHeader* Payload::*;
    static constexpr std::array<Field, 0> kFields{};
};

using Object = test_support::Object<Payload>;

} // namespace

TEST(ExceptionTest, ProcessUnhandledException_WithHook) {
    test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>().setSuperType(theThrowableTypeInfo)};
    kotlin::RunInNewThread([&typeHolder]() {
        Object exception(typeHolder.typeInfo());
        exception.header()->typeInfoOrMeta_ = setPointerBits(exception.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        exception->value = 42;
        auto reportUnhandledExceptionMock = ScopedReportUnhandledExceptionMock();
        auto Kotlin_runUnhandledExceptionHookMock = ScopedKotlin_runUnhandledExceptionHookMock();
        EXPECT_CALL(*reportUnhandledExceptionMock, Call(_)).Times(0);
        EXPECT_CALL(*Kotlin_runUnhandledExceptionHookMock, Call(_)).WillOnce([](KRef exception) {
            EXPECT_THAT(Object::FromObjHeader(exception)->value, 42);
        });
        kotlin::ProcessUnhandledException(exception.header());
    });
}

TEST(ExceptionDeathTest, ProcessUnhandledException_NoHook) {
    test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>().setSuperType(theThrowableTypeInfo)};
    kotlin::RunInNewThread([&typeHolder]() {
        Object exception(typeHolder.typeInfo());
        exception.header()->typeInfoOrMeta_ = setPointerBits(exception.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        exception->value = 42;
        auto reportUnhandledExceptionMock = ScopedReportUnhandledExceptionMock();
        auto Kotlin_runUnhandledExceptionHookMock = ScopedKotlin_runUnhandledExceptionHookMock();
        ON_CALL(*reportUnhandledExceptionMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Reporting %d\n", Object::FromObjHeader(exception)->value);
        });
        ON_CALL(*Kotlin_runUnhandledExceptionHookMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Hook %d\n", Object::FromObjHeader(exception)->value);
            // Kotlin_runUnhandledExceptionHookMock rethrows original exception when hook is unset.
            ThrowException(exception);
        });
        EXPECT_DEATH({ kotlin::ProcessUnhandledException(exception.header()); }, "Hook 42\nReporting 42\n");
    });
}

TEST(ExceptionDeathTest, ProcessUnhandledException_WithFailingHook) {
    test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>().setSuperType(theThrowableTypeInfo)};
    kotlin::RunInNewThread([&typeHolder]() {
        Object exception(typeHolder.typeInfo());
        exception.header()->typeInfoOrMeta_ = setPointerBits(exception.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        exception->value = 42;
        Object hookException(typeHolder.typeInfo());
        hookException.header()->typeInfoOrMeta_ = setPointerBits(hookException.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        hookException->value = 13;
        auto reportUnhandledExceptionMock = ScopedReportUnhandledExceptionMock();
        auto Kotlin_runUnhandledExceptionHookMock = ScopedKotlin_runUnhandledExceptionHookMock();
        ON_CALL(*reportUnhandledExceptionMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Reporting %d\n", Object::FromObjHeader(exception)->value);
        });
        ON_CALL(*Kotlin_runUnhandledExceptionHookMock, Call(_)).WillByDefault([&hookException](KRef exception) {
            konan::consoleErrorf("Hook %d\n", Object::FromObjHeader(exception)->value);
            ThrowException(hookException.header());
        });
        EXPECT_DEATH({ kotlin::ProcessUnhandledException(exception.header()); }, "Hook 42\nReporting 13\n");
    });
}

TEST(ExceptionDeathTest, ProcessUnhandledException_WithTerminatingFailingHook) {
    test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>().setSuperType(theThrowableTypeInfo)};
    kotlin::RunInNewThread([&typeHolder]() {
        Object exception(typeHolder.typeInfo());
        exception.header()->typeInfoOrMeta_ = setPointerBits(exception.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        exception->value = 42;
        Object hookException(typeHolder.typeInfo());
        hookException.header()->typeInfoOrMeta_ = setPointerBits(hookException.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        hookException->value = 13;
        auto reportUnhandledExceptionMock = ScopedReportUnhandledExceptionMock();
        auto Kotlin_runUnhandledExceptionHookMock = ScopedKotlin_runUnhandledExceptionHookMock();
        ON_CALL(*reportUnhandledExceptionMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Reporting %d\n", Object::FromObjHeader(exception)->value);
        });
        ON_CALL(*Kotlin_runUnhandledExceptionHookMock, Call(_)).WillByDefault([&hookException](KRef exception) {
            konan::consoleErrorf("Hook %d\n", Object::FromObjHeader(exception)->value);
            kotlin::TerminateWithUnhandledException(hookException.header());
        });
        EXPECT_DEATH({ kotlin::ProcessUnhandledException(exception.header()); }, "Hook 42\nReporting 13\n");
    });
}

TEST(ExceptionDeathTest, TerminateWithUnhandledException) {
    test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>().setSuperType(theThrowableTypeInfo)};
    kotlin::RunInNewThread([&typeHolder]() {
        Object exception(typeHolder.typeInfo());
        exception.header()->typeInfoOrMeta_ = setPointerBits(exception.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        exception->value = 42;
        auto reportUnhandledExceptionMock = ScopedReportUnhandledExceptionMock();
        auto Kotlin_runUnhandledExceptionHookMock = ScopedKotlin_runUnhandledExceptionHookMock();
        ON_CALL(*reportUnhandledExceptionMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Reporting %d\n", Object::FromObjHeader(exception)->value);
        });
        ON_CALL(*Kotlin_runUnhandledExceptionHookMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Hook %d\n", Object::FromObjHeader(exception)->value);
        });
        EXPECT_DEATH({ kotlin::TerminateWithUnhandledException(exception.header()); }, "Reporting 42\n");
    });
}

TEST(ExceptionDeathTest, TerminateHandler_WithHook) {
    test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>().setSuperType(theThrowableTypeInfo)};
    kotlin::RunInNewThread([&typeHolder]() {
        Object exception(typeHolder.typeInfo());
        exception.header()->typeInfoOrMeta_ = setPointerBits(exception.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        exception->value = 42;
        auto reportUnhandledExceptionMock = ScopedReportUnhandledExceptionMock();
        auto Kotlin_runUnhandledExceptionHookMock = ScopedKotlin_runUnhandledExceptionHookMock();
        ON_CALL(*reportUnhandledExceptionMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Reporting %d\n", Object::FromObjHeader(exception)->value);
        });
        ON_CALL(*Kotlin_runUnhandledExceptionHookMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Hook %d\n", Object::FromObjHeader(exception)->value);
        });
        EXPECT_DEATH(
                {
                    std::set_terminate([]() {
                        konan::consoleErrorf("Custom terminate\n");
                        if (auto exception = std::current_exception()) {
                            try {
                                std::rethrow_exception(exception);
                            } catch (int i) {
                                konan::consoleErrorf("Exception %d\n", i);
                            } catch (...) {
                                konan::consoleErrorf("Unknown Exception\n");
                            }
                        }
                    });
                    SetKonanTerminateHandler();
                    try {
                        ThrowException(exception.header());
                    } catch (...) {
                        std::terminate();
                    }
                },
                "Hook 42\n");
    });
}

TEST(ExceptionDeathTest, TerminateHandler_NoHook) {
    test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>().setSuperType(theThrowableTypeInfo)};
    kotlin::RunInNewThread([&typeHolder]() {
        Object exception(typeHolder.typeInfo());
        exception.header()->typeInfoOrMeta_ = setPointerBits(exception.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        exception->value = 42;
        auto reportUnhandledExceptionMock = ScopedReportUnhandledExceptionMock();
        auto Kotlin_runUnhandledExceptionHookMock = ScopedKotlin_runUnhandledExceptionHookMock();
        ON_CALL(*reportUnhandledExceptionMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Reporting %d\n", Object::FromObjHeader(exception)->value);
        });
        ON_CALL(*Kotlin_runUnhandledExceptionHookMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Hook %d\n", Object::FromObjHeader(exception)->value);
            // Kotlin_runUnhandledExceptionHookMock rethrows original exception when hook is unset.
            ThrowException(exception);
        });
        EXPECT_DEATH(
                {
                    std::set_terminate([]() {
                        konan::consoleErrorf("Custom terminate\n");
                        if (auto exception = std::current_exception()) {
                            try {
                                std::rethrow_exception(exception);
                            } catch (int i) {
                                konan::consoleErrorf("Exception %d\n", i);
                            } catch (...) {
                                konan::consoleErrorf("Unknown Exception\n");
                            }
                        }
                    });
                    SetKonanTerminateHandler();
                    try {
                        ThrowException(exception.header());
                    } catch (...) {
                        std::terminate();
                    }
                },
                "Hook 42\nReporting 42\n");
    });
}

TEST(ExceptionDeathTest, TerminateHandler_WithFailingHook) {
    test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>().setSuperType(theThrowableTypeInfo)};
    kotlin::RunInNewThread([&typeHolder]() {
        Object exception(typeHolder.typeInfo());
        exception.header()->typeInfoOrMeta_ = setPointerBits(exception.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        exception->value = 42;
        Object hookException(typeHolder.typeInfo());
        hookException.header()->typeInfoOrMeta_ = setPointerBits(hookException.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        hookException->value = 13;
        auto reportUnhandledExceptionMock = ScopedReportUnhandledExceptionMock();
        auto Kotlin_runUnhandledExceptionHookMock = ScopedKotlin_runUnhandledExceptionHookMock();
        ON_CALL(*reportUnhandledExceptionMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Reporting %d\n", Object::FromObjHeader(exception)->value);
        });
        ON_CALL(*Kotlin_runUnhandledExceptionHookMock, Call(_)).WillByDefault([&hookException](KRef exception) {
            konan::consoleErrorf("Hook %d\n", Object::FromObjHeader(exception)->value);
            ThrowException(hookException.header());
        });
        EXPECT_DEATH(
                {
                    std::set_terminate([]() {
                        konan::consoleErrorf("Custom terminate\n");
                        if (auto exception = std::current_exception()) {
                            try {
                                std::rethrow_exception(exception);
                            } catch (int i) {
                                konan::consoleErrorf("Exception %d\n", i);
                            } catch (...) {
                                konan::consoleErrorf("Unknown Exception\n");
                            }
                        }
                    });
                    SetKonanTerminateHandler();
                    try {
                        ThrowException(exception.header());
                    } catch (...) {
                        std::terminate();
                    }
                },
                "Hook 42\nReporting 13\n");
    });
}

TEST(ExceptionDeathTest, TerminateHandler_IgnoreHooks) {
    kotlin::RunInNewThread([]() {
        auto reportUnhandledExceptionMock = ScopedReportUnhandledExceptionMock();
        auto Kotlin_runUnhandledExceptionHookMock = ScopedKotlin_runUnhandledExceptionHookMock();
        ON_CALL(*reportUnhandledExceptionMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Reporting %d\n", Object::FromObjHeader(exception)->value);
        });
        ON_CALL(*Kotlin_runUnhandledExceptionHookMock, Call(_)).WillByDefault([](KRef exception) {
            konan::consoleErrorf("Hook %d\n", Object::FromObjHeader(exception)->value);
            ThrowException(exception);
        });
        EXPECT_DEATH(
                {
                    std::set_terminate([]() {
                        konan::consoleErrorf("Custom terminate\n");
                        if (auto exception = std::current_exception()) {
                            try {
                                std::rethrow_exception(exception);
                            } catch (int i) {
                                konan::consoleErrorf("Exception %d\n", i);
                            } catch (...) {
                                konan::consoleErrorf("Unknown Exception\n");
                            }
                        }
                    });
                    SetKonanTerminateHandler();
                    try {
                        throw 3;
                    } catch (...) {
                        std::terminate();
                    }
                },
                "Custom terminate\nException 3\n");
    });
}
