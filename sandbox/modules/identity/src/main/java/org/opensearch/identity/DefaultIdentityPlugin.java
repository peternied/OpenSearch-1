/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.support.ActionFilter;
import org.opensearch.identity.AuthManager;
import org.opensearch.identity.Identity;
import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.identity.authmanager.internal.InternalAuthenticationManager;
import org.opensearch.identity.configuration.ClusterInfoHolder;
import org.opensearch.identity.configuration.ConfigurationRepository;
import org.opensearch.identity.configuration.DynamicConfigFactory;
import org.opensearch.indices.SystemIndexDescriptor;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.ClusterPlugin;
import org.opensearch.plugins.IdentityPlugin;
import org.opensearch.plugins.NetworkPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.SystemIndexPlugin;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.rest.RestHandler;
import org.opensearch.script.ScriptService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class DefaultIdentityPlugin extends Plugin implements IdentityPlugin, ActionPlugin, NetworkPlugin, SystemIndexPlugin, ClusterPlugin {
    private volatile Logger log = LogManager.getLogger(this.getClass());

    private volatile SecurityRestFilter securityRestHandler;

    private final boolean enabled;
    private volatile Settings settings;

    private volatile Path configPath;
    private volatile ThreadPool threadPool;

    private volatile ConfigurationRepository cr;
    private volatile ClusterService cs;
    private volatile Client localClient;
    private volatile NamedXContentRegistry namedXContentRegistry = null;

    @SuppressWarnings("removal")
    public IdentityPlugin(final Settings settings, final Path configPath) {
        enabled = isEnabled(settings);

        if (!enabled) {
            log.warn("Identity module is disabled.");
            return;
        }

        this.configPath = configPath;

        if (this.configPath != null) {
            log.info("OpenSearch Config path is {}", this.configPath.toAbsolutePath());
        } else {
            log.info("OpenSearch Config path is not set");
        }

        this.settings = settings;
    }

    private static boolean isEnabled(final Settings settings) {
        return settings.getAsBoolean(ConfigConstants.IDENTITY_ENABLED, false);
    }

    @Override
    public UnaryOperator<RestHandler> getRestHandlerWrapper(final ThreadContext threadContext) {
        if (!enabled) {
            return (rh) -> rh;
        }
        return (rh) -> securityRestHandler.wrap(rh);
    }

    @Override
    public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
        final String indexPattern = settings.get(ConfigConstants.IDENTITY_CONFIG_INDEX_NAME, ConfigConstants.IDENTITY_DEFAULT_CONFIG_INDEX);
        final SystemIndexDescriptor systemIndexDescriptor = new SystemIndexDescriptor(indexPattern, "Identity index");
        return Collections.singletonList(systemIndexDescriptor);
    }

    public List<Setting<?>> getSettings() {
        List<Setting<?>> settings = new ArrayList<Setting<?>>();
        settings.addAll(super.getSettings());
        settings.add(Setting.boolSetting(ConfigConstants.IDENTITY_ENABLED, false, Setting.Property.NodeScope, Setting.Property.Filtered));
        settings.add(
            Setting.simpleString(
                ConfigConstants.IDENTITY_AUTH_MANAGER_CLASS,
                InternalAuthenticationManager.class.getCanonicalName(),
                Setting.Property.NodeScope,
                Setting.Property.Filtered
            )
        );
        settings.add(
            Setting.simpleString(ConfigConstants.IDENTITY_CONFIG_INDEX_NAME, Setting.Property.NodeScope, Setting.Property.Filtered)
        );

        return settings;
    }

    @Override
    public void onNodeStarted() {
        log.info("Node started");
        if (enabled) {
            cr.initOnNodeStart();
        }
    }

    @Override
    public Collection<Object> createComponents(
        Client localClient,
        ClusterService clusterService,
        ThreadPool threadPool,
        ResourceWatcherService resourceWatcherService,
        ScriptService scriptService,
        NamedXContentRegistry xContentRegistry,
        Environment environment,
        NodeEnvironment nodeEnvironment,
        NamedWriteableRegistry namedWriteableRegistry,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<RepositoriesService> repositoriesServiceSupplier
    ) {
        // TODO The constructor is not getting called in time leaving these values as null when creating the ConfigurationRepository
        // Can the constructor be substituted by taking these from environment?
        this.configPath = environment.configDir();
        this.settings = environment.settings();

        // TODO: revisit this
        final String authManagerClassName = this.settings.get(
            ConfigConstants.IDENTITY_AUTH_MANAGER_CLASS,
            InternalAuthenticationManager.class.getCanonicalName()
        );
        AuthManager authManager = null;
        try {
            Class<?> clazz = Class.forName(authManagerClassName);
            authManager = (AuthManager) clazz.getConstructor().newInstance();

            try {
                Method method = clazz.getMethod("setThreadPool", ThreadPool.class);
                method.invoke(authManager, threadPool);
            } catch (NoSuchMethodException e) {
                /** ignore */
            }

            IdentityModule.setModule(authManager);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        this.threadPool = threadPool;
        this.cs = clusterService;
        this.localClient = localClient;

        final List<Object> components = new ArrayList<Object>();

        if (!enabled) {
            return components;
        }

        final ClusterInfoHolder cih = new ClusterInfoHolder();
        this.cs.addListener(cih);

        securityRestHandler = new SecurityRestFilter(threadPool, settings, configPath);

        cr = ConfigurationRepository.create(settings, this.configPath, threadPool, localClient, clusterService);

        final DynamicConfigFactory dcf = new DynamicConfigFactory(cr, settings, configPath, localClient, threadPool, cih);
        // TODO Register DCF listeners to dynamically load config
        // dcf.registerDCFListener(securityRestHandler);

        cr.setDynamicConfigFactory(dcf);

        return components;
    }
}
