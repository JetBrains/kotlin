/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import { KarmaWebpackOutputFramework } from "../src/KarmaWebpackOutputFramework.mjs";
import assert from "node:assert";

describe('WebpackOutputFramework', () => {
    it('Defaults', () => {
        const controller = {outputPath: 'foo/'};
        const config = {files: [], __karmaWebpackController: controller};

        KarmaWebpackOutputFramework(config);

        assert.equal(config.files.length, 1, "Expected file length should be 1");
        assert.deepEqual(
            config.files,
            [
                {
                    pattern: `${controller.outputPath}/**/*`,
                    included: false,
                    served: true,
                    watched: false
                }
            ]
        );
    });
});