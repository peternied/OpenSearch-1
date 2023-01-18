/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.authmanager.internal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.opensearch.identity.tokens.BasicAuthToken;
import org.opensearch.identity.tokens.BearerAuthToken;
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
    public static AuthenticationToken extractShiroAuthToken(org.opensearch.identity.tokens.AuthenticationToken authenticationToken) {
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

        final byte[] decodedAuthHeader = Base64.getDecoder().decode(token.getHeaderValue().substring("Basic".length()).trim());
        String decodedHeader = new String(decodedAuthHeader, StandardCharsets.UTF_8);

        final int firstColonIndex = decodedHeader.indexOf(':');

        String username = null;
        String password = null;

        if (firstColonIndex > 0) {
            username = decodedHeader.substring(0, firstColonIndex);

            if (decodedHeader.length() - 1 != firstColonIndex) {
                password = decodedHeader.substring(firstColonIndex + 1);
            } else {
                // blank password
                password = "";
            }
        }

        if (username == null || password == null) {
            logger.warn("Invalid 'Authorization' header, send 401 and 'WWW-Authenticate Basic'");
            return null;
        }

        logger.info("Logging in as: " + username);

        return new UsernamePasswordToken(username, password);
    }
}
