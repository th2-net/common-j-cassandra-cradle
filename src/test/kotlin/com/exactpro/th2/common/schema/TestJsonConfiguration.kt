/*
 * Copyright 2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.common.schema

import com.exactpro.th2.CradleConfidentialConfiguration
import com.exactpro.th2.CradleNonConfidentialConfiguration
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Path

class TestJsonConfiguration {

    @Test
    fun `test cradle confidential json configuration deserialize`() {
        testDeserialize(CRADLE_CONFIDENTIAL_CONF_JSON, CRADLE_CONFIDENTIAL_CONF)
    }

    @Test
    fun `test cradle confidential json configuration serialize and deserialize`() {
        testSerializeAndDeserialize(CRADLE_CONFIDENTIAL_CONF)
    }

    @Test
    fun `test cradle non confidential json configuration serialize and deserialize`() {
        testSerializeAndDeserialize(CRADLE_NON_CONFIDENTIAL_CONF)
    }

    @Test
    fun `test cradle non confidential json configuration deserialize`() {
        testDeserialize(CRADLE_NON_CONFIDENTIAL_CONF_JSON, CRADLE_NON_CONFIDENTIAL_CONF)
    }

    private fun testSerializeAndDeserialize(configuration: Any) {
        OBJECT_MAPPER.writeValueAsString(configuration).also { jsonString ->
            testDeserialize(jsonString, configuration)
        }
    }

    private fun testDeserialize(json: String, obj: Any) {
        val value = OBJECT_MAPPER.readValue(json, obj::class.java)
        Assertions.assertEquals(obj, value)
    }

    companion object {
        @JvmStatic
        private val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())

        @JvmStatic
        private val CONF_DIR = Path.of("test_json_configurations")

        private val CRADLE_CONFIDENTIAL_CONF_JSON = loadConfJson("cradle_confidential")
        private val CRADLE_CONFIDENTIAL_CONF = CradleConfidentialConfiguration(
            "data center",
            "host",
            "keyspace",
            1234,
            "user",
            "pass",
            "instance"
        )

        private val CRADLE_NON_CONFIDENTIAL_CONF_JSON = loadConfJson("cradle_non_confidential")
        private val CRADLE_NON_CONFIDENTIAL_CONF = CradleNonConfidentialConfiguration(
            888,
            111,
            123,
            321,
            false
        )

        init {
            OBJECT_MAPPER.registerKotlinModule()
        }

        private fun loadConfJson(fileName: String): String {
            val path = CONF_DIR.resolve(fileName)

            return Thread.currentThread().contextClassLoader
                .getResourceAsStream("$path.json")?.readAllBytes()?.let { bytes -> String(bytes) }
                ?: error("Can not load resource by path $path.json")
        }
    }
}