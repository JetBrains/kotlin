/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.klint.checks;


import com.android.annotations.NonNull;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Main entry point for API description.
 *
 * To create the {@link Api}, use {@link #parseApi(File)}
 *
 */
public class Api {

    /**
     * Parses simplified API file.
     * @param apiFile the file to read
     * @return a new ApiInfo
     */
    public static Api parseApi(File apiFile) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(apiFile);
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser parser = parserFactory.newSAXParser();
            ApiParser apiParser = new ApiParser();
            parser.parse(inputStream, apiParser);
            inputStream.close();

            // Also read in API (unless regenerating the map for newer libraries)
            //noinspection PointlessBooleanExpression,TestOnlyProblems
            if (!ApiLookup.DEBUG_FORCE_REGENERATE_BINARY) {
                inputStream = Api.class.getResourceAsStream("api-versions-support-library.xml");
                if (inputStream != null) {
                    parser.parse(inputStream, apiParser);
                }
            }

            return new Api(apiParser.getClasses(), apiParser.getPackages());
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return null;
    }

    private final Map<String, ApiClass> mClasses;
    private final Map<String, ApiPackage> mPackages;

    private Api(
            @NonNull Map<String, ApiClass> classes,
            @NonNull Map<String, ApiPackage> packages) {
        mClasses = new HashMap<String, ApiClass>(classes);
        mPackages = new HashMap<String, ApiPackage>(packages);
    }

    ApiClass getClass(String fqcn) {
        return mClasses.get(fqcn);
    }

    Map<String, ApiClass> getClasses() {
        return Collections.unmodifiableMap(mClasses);
    }

    Map<String, ApiPackage> getPackages() {
        return Collections.unmodifiableMap(mPackages);
    }
}
