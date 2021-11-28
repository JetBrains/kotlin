/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class AnyInstance {}

let OBJ = AnyInstance()

class ParameterNotNullAssertionBenchmark {
    
    func methodWithOneNotnullParameter(_ p: Any) -> Any {
        return p
    }

    private func privateMethodWithOneNotnullParameter(_ p: Any) -> Any {
        return p
    }

    func methodWithTwoNotnullParameters(_ p: Any, _ p2: Any) -> Any {
        return p
    }

    private func privateMethodWithTwoNotnullParameters(_ p: Any, _ p2: Any) -> Any {
        return p
    }

    func methodWithEightNotnullParameters(_ p: Any, _ p2: Any, _ p3: Any, _ p4: Any, _ p5: Any, _ p6: Any, _ p7: Any, _ p8: Any) -> Any {
        return p
    }

    private func privateMethodWithEightNotnullParameters(_ p: Any, _ p2: Any, _ p3: Any, _ p4: Any, _ p5: Any, _ p6: Any, _ p7: Any, _ p8: Any) -> Any {
        return p
    }

    func invokeOneArgWithNullCheck() -> Any {
        return methodWithOneNotnullParameter(OBJ)
    }

    func invokeOneArgWithoutNullCheck() -> Any {
        return privateMethodWithOneNotnullParameter(OBJ)
    }

    func invokeTwoArgsWithNullCheck() -> Any {
        return methodWithTwoNotnullParameters(OBJ, OBJ)
    }

    func invokeTwoArgsWithoutNullCheck() -> Any {
        return privateMethodWithTwoNotnullParameters(OBJ, OBJ)
    }

    func invokeEightArgsWithNullCheck() -> Any {
        return methodWithEightNotnullParameters(OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ)
    }

    func invokeEightArgsWithoutNullCheck() -> Any {
        return privateMethodWithEightNotnullParameters(OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ)
    }
}
