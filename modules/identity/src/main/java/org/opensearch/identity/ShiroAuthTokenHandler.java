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
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.opensearch.identity.tokens.BasicAuthToken;

/**
 * Extracts Shiro's {@link AuthenticationToken} from different types of auth headers
 *
 * @opensearch.experimental
 */
public class ShiroAuthTokenHandler {

    private static final Logger logger = LogManager.getLogger(ShiroAuthTokenHandler.class);

    /**
     * Extracts shiro auth token from the given header token
     * @param authenticationToken the token from which to extract
     * @return the extracted shiro auth token to be used to perform login
     */
    public static AuthenticationToken translateAuthToken(org.opensearch.identity.tokens.AuthToken authenticationToken) {
        AuthenticationToken authToken = null;

        if (authenticationToken instanceof BasicAuthToken) {
            final BasicAuthToken basicAuthToken = (BasicAuthToken) authenticationToken;
            return new UsernamePasswordToken(basicAuthToken.getUser(), basicAuthToken.getPassword());
        }

        return authToken;
    }
}
