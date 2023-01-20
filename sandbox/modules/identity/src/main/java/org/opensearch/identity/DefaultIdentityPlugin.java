/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity;

import org.opensearch.identity.realm.InternalRealm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.plugins.IdentityPlugin;
import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.IdentityPlugin;
import org.opensearch.plugins.Plugin;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.DefaultSecurityManager;

public final class DefaultIdentityPlugin extends Plugin implements IdentityPlugin {
    private volatile Logger log = LogManager.getLogger(this.getClass());

    private volatile Settings settings;

    @SuppressWarnings("removal")
    public DefaultIdentityPlugin(final Settings settings) {
        this.settings = settings;

        SecurityManager securityManager = new DefaultSecurityManager(InternalRealm.INSTANCE);
        SecurityUtils.setSecurityManager(securityManager);
    }

    @Override
    public Subject getSubject() {
        return new InternalSubject(SecurityUtils.getSubject());
    }
}
