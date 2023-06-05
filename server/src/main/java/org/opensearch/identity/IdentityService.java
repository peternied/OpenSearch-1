/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.identity;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.OpenSearchException;
import org.opensearch.common.settings.Settings;
import org.opensearch.identity.noop.NoopIdentityPlugin;
import org.opensearch.plugins.IdentityPlugin;

/**
 * Identity and access control for OpenSearch.
 *
 * @opensearch.experimental
 * */
public class IdentityService {
    private static final Logger log = LogManager.getLogger(IdentityService.class);

    private final Settings settings;
    private final IdentityPlugin identityPlugin;

    private static IdentityService instance = null;

    public static IdentityService getInstance() {
        if (instance == null) {
            new IdentityService(Settings.EMPTY, List.of());
        }
        return instance;
    }

    public IdentityService(final Settings settings, final List<IdentityPlugin> identityPlugins) {
        this.settings = settings;

        if (identityPlugins.size() == 0) {
            log.debug("Identity plugins size is 0");
            identityPlugin = new NoopIdentityPlugin();
        } else if (identityPlugins.size() == 1) {
            log.debug("Identity plugins size is 1");
            identityPlugin = identityPlugins.get(0);
        } else {
            throw new OpenSearchException(
                "Multiple identity plugins are not supported, found: "
                    + identityPlugins.stream().map(Object::getClass).map(Class::getName).collect(Collectors.joining(","))
            );
        }
        IdentityService.instance = this;
    }

    /**
     * Gets the current subject
     */
    public Subject getSubject() {
        return identityPlugin.getSubject();
    }
}
