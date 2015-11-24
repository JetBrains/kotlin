/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
module.exports = function (config) {
    config.set({
            frameworks: ['qunit'],
            reporters: ['progress', 'junit'],
            files: [
                '../../../target/test-js/kotlin.js',
                '../../../target/test-js/*.js',
                '../../../target/classes/*.js'
            ],
            exclude: [],
            port: 9876,
            runnerPort: 9100,
            colors: true,
            autoWatch: false,
            browsers: [
                'PhantomJS'
            ],
            captureTimeout: 5000,
            //singleRun: false,
            singleRun: true,
            reportSlowerThan: 500,

            junitReporter: {
                outputFile: '../../../target/reports/test-results.xml',
                suite: ''
            },
            preprocessors: {
                '**/*.js': ['sourcemap']
            }
        }
    )
};