/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.identity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.OpenSearchException;
import org.opensearch.identity.noop.NoopIdentityPlugin;
import java.util.List;
import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.IdentityPlugin;
import java.util.stream.Collectors;

/**
 * Identity and access control for OpenSearch.
 *
 * @opensearch.experimental
 * */
public class IdentityModule {
    private static final Logger logger = LogManager.getLogger(IdentityModule.class);

    /** Application wide access for identity systems */
    private static IdentityModule IDENTITY_MODULE = null;

    /**
     * Gets the Identity Service for this application
     */
    public static IdentityModule getModule() {
        return IDENTITY_MODULE;
    }

    /**
     * Gets the Identity Service for this application
     */
    public static void setModule(final IdentityModule module) {
        IDENTITY_MODULE = module;
    }

    private final Settings settings;
    private final IdentityPlugin identityPlugin;

    public IdentityModule(final Settings settings, final List<IdentityPlugin> identityPlugins) {
        this.settings = settings;

        if (identityPlugins.size() == 0) {
            identityPlugin = new NoopIdentityPlugin();
        } else if (identityPlugins.size() == 1) {
            identityPlugin = identityPlugins.get(0);
        } else {
            throw new OpenSearchException(
                "Multiple identity plugins are not supported, found: "
                    + identityPlugins.stream().map(Object::getClass).map(Class::getName).collect(Collectors.joining(","))
            );
        }

        if (IDENTITY_MODULE == null) {
            IDENTITY_MODULE = this;
        }

        logger.info("Identity module loaded with " + identityPlugin.getClass().getName());
        // logger.info("Current subject " + getSubject());
    }

    /**
     * Gets the current subject
     */
    public Subject getSubject() {
        return identityPlugin.getSubject();
    }
}
