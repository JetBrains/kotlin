/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.elastic

import kotlin.js.Promise            // TODO - migrate to multiplatform.
import org.jetbrains.report.json.*
import org.jetbrains.network.*

// Connector with InfluxDB.
class ElasticSearchConnector(private val connector: NetworkConnector,
                             private val user: String? = null, private val password: String? = null) {
    // Execute ElasticSearch request.
    fun request(method: RequestMethod, path: String, acceptJsonContentType: Boolean = true, body: String? = null) =
            connector.sendRequest(method, path, user, password, acceptJsonContentType, body)
}