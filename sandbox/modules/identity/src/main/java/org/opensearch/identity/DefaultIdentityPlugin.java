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
import org.opensearch.plugins.IdentityPlugin;
import org.opensearch.identity.IdentityModule;
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
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.util.Factory;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;

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

    private final boolean enabled;
    private volatile Settings settings;

    private volatile Path configPath;
    private volatile ThreadPool threadPool;

    private volatile ClusterService cs;
    private volatile Client localClient;
    private volatile NamedXContentRegistry namedXContentRegistry = null;

    @SuppressWarnings("removal")
    public DefaultIdentityPlugin(final Settings settings, final Path configPath) {
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

        Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.ini");
        SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);
    }

    private static boolean isEnabled(final Settings settings) {
        return settings.getAsBoolean(ConfigConstants.IDENTITY_ENABLED, false);
    }

    @Override
    public void onNodeStarted() {
        log.info("Node started");
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

        this.threadPool = threadPool;
        this.cs = clusterService;
        this.localClient = localClient;

        final List<Object> components = new ArrayList<Object>();

        if (!enabled) {
            return components;
        }

        return components;
    }

    @Override
    public Subject getSubject() {
        return new InternalSubject(SecurityUtils.getSubject());
    }
}
