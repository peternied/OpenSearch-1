/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.shiro;

import org.opensearch.identity.Subject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.plugins.IdentityPlugin;
import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.Plugin;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;

/**
 * Identity implementation with Shiro
 *
 * @opensearch.experimental
 */
public final class ShiroIdentityPlugin extends Plugin implements IdentityPlugin {
    private volatile Logger log = LogManager.getLogger(this.getClass());

    private volatile Settings settings;

    public ShiroIdentityPlugin(final Settings settings) {
        this.settings = settings;

        SecurityManager securityManager = new OpenSearchSecurityManager();
        SecurityUtils.setSecurityManager(securityManager);
    }

    @Override
    public Subject getSubject() {
        return new ShiroSubject(SecurityUtils.getSubject());
    }
}
