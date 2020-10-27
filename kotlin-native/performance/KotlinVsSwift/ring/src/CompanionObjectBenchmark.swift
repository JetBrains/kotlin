/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class CompanionObjectBenchmark {
    func invokeRegularFunction() {
        CompanionObjectBenchmark.regularCompanionObjectFunction("")
    }
    
    static func regularCompanionObjectFunction(_ o: Any) -> Any {
        return o
    }
}
