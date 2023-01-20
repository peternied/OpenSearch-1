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
import org.opensearch.identity.tokens.AuthToken;
import org.apache.shiro.authc.BearerToken;


import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Extracts Shiro's {@link AuthenticationToken} from different types of auth headers
 *
 * @opensearch.experimental
 */
public class AuthenticationTokenHandler {

    private static final Logger logger = LogManager.getLogger(AuthenticationTokenHandler.class);

    /**
     * Extracts shiro auth token from the given header token
     * @param authenticationToken the token from which to extract
     * @return the extracted shiro auth token to be used to perform login
     */
    public static AuthenticationToken extractShiroAuthToken(org.opensearch.identity.tokens.AuthToken authenticationToken) {
        AuthenticationToken authToken = null;

        if (authenticationToken instanceof BasicAuthToken) {
            authToken = handleBasicAuth((BasicAuthToken) authenticationToken);
        }

        return authToken;
    }

    /**
     * Returns auth token extracted from basic auth header
     * @param token the basic auth token
     * @return the extracted auth token
     */
    private static AuthenticationToken handleBasicAuth(final BasicAuthToken token) {

        logger.info("Logging in as: " + token.getUser());

        return new UsernamePasswordToken(token.getUser(), token.getPassword());
    }
}
