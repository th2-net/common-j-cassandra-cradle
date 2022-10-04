/*
 * Copyright 2020-2022 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.common.schema.factory;

import com.exactpro.cradle.CradleManager;
import com.exactpro.cradle.cassandra.CassandraCradleManager;
import com.exactpro.cradle.cassandra.CassandraStorageSettings;
import com.exactpro.cradle.cassandra.connection.CassandraConnection;
import com.exactpro.cradle.cassandra.connection.CassandraConnectionSettings;
import com.exactpro.cradle.utils.CradleStorageException;
import com.exactpro.th2.CradleConfidentialConfiguration;
import com.exactpro.th2.CradleNonConfidentialConfiguration;
import com.exactpro.th2.common.ConfigurationProvider;
import com.exactpro.th2.common.ModuleFactory;
import com.exactpro.th2.common.schema.configuration.Configuration;
import com.google.auto.service.AutoService;
import kotlin.collections.SetsKt;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@AutoService(ModuleFactory.class)
public class CradleManagerFactoryImpl implements ModuleFactory {

    protected static final String DEFAULT_CRADLE_INSTANCE_NAME = "infra";
    private final String CRADLE_CONFIDENTIAL_ID = "cradle_confidential";
    private final String CRADLE_NON_CONFIDENTIAL_ID = "cradle_non_confidential";

    @Override
    public Set<Class<?>> getModuleClasses() {
        return SetsKt.hashSetOf(CradleManager.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Class<? extends Configuration>> getConfigurationClasses() {
        return SetsKt.hashSetOf(CradleConfidentialConfiguration.class, CradleNonConfidentialConfiguration.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <M> M loadModule(ConfigurationProvider configurationProvider, Class<M> aClass) {
        if (aClass.equals(CradleManager.class)) {
            return (M) loadCradleManager(configurationProvider);
        } else {
            throw new IllegalArgumentException("CradleManagerFactory doesn't support " + aClass);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Configuration> C loadConfiguration(ConfigurationProvider configurationProvider, Class<C> aClass) {
        if (aClass.equals(CradleConfidentialConfiguration.class)) {
            return (C) configurationProvider.loadConfiguration(CRADLE_CONFIDENTIAL_ID, CradleConfidentialConfiguration.class);
        } else if (aClass.equals(CradleNonConfidentialConfiguration.class)) {
            return (C) configurationProvider.loadConfiguration(CRADLE_NON_CONFIDENTIAL_ID, CradleNonConfidentialConfiguration.class);
        } else {
            throw new IllegalArgumentException("CradleManagerFactory doesn't support " + aClass);
        }
    }

    @NotNull
    private CradleManager loadCradleManager(@NotNull ConfigurationProvider configurationProvider) {
        try {
            CradleConfidentialConfiguration confidentialConfiguration =
                    loadConfiguration(configurationProvider, CradleConfidentialConfiguration.class);
            CradleNonConfidentialConfiguration nonConfidentialConfiguration =
                    loadConfiguration(configurationProvider, CradleNonConfidentialConfiguration.class);

            CassandraConnectionSettings cassandraConnectionSettings = new CassandraConnectionSettings(
                    confidentialConfiguration.getDataCenter(),
                    confidentialConfiguration.getHost(),
                    confidentialConfiguration.getPort(),
                    confidentialConfiguration.getKeyspace());

            if (StringUtils.isNotEmpty(confidentialConfiguration.getUsername())) {
                cassandraConnectionSettings.setUsername(confidentialConfiguration.getUsername());
            }

            if (StringUtils.isNotEmpty(confidentialConfiguration.getPassword())) {
                cassandraConnectionSettings.setPassword(confidentialConfiguration.getPassword());
            }

            if (nonConfidentialConfiguration.getTimeout() > 0) {
                cassandraConnectionSettings.setTimeout(nonConfidentialConfiguration.getTimeout());
            }

            if (nonConfidentialConfiguration.getPageSize() > 0) {
                cassandraConnectionSettings.setResultPageSize(nonConfidentialConfiguration.getPageSize());
            }

            CradleManager manager = new CassandraCradleManager(new CassandraConnection(cassandraConnectionSettings));
            manager.init(
                    defaultIfBlank(confidentialConfiguration.getCradleInstanceName(), DEFAULT_CRADLE_INSTANCE_NAME),
                    nonConfidentialConfiguration.getPrepareStorage(),
                    nonConfidentialConfiguration.getCradleMaxMessageBatchSize() > 0
                            ? nonConfidentialConfiguration.getCradleMaxMessageBatchSize()
                            : CassandraStorageSettings.DEFAULT_MAX_MESSAGE_BATCH_SIZE,
                    nonConfidentialConfiguration.getCradleMaxEventBatchSize() > 0
                            ? nonConfidentialConfiguration.getCradleMaxEventBatchSize()
                            : CassandraStorageSettings.DEFAULT_MAX_EVENT_BATCH_SIZE
            );
            return manager;
        } catch (CradleStorageException | RuntimeException e) {
            throw new IllegalArgumentException("Cannot create Cradle manager", e);
        }
    }

}
