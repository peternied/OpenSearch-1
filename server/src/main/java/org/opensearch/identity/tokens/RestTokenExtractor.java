/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.identity.tokens;

import org.opensearch.rest.RestRequest;

import java.util.Collections;
import java.util.Optional;

/**
 * Extracts tokens from RestRequests used for authentication
 */
public class RestTokenExtractor {

    public final static String AUTH_HEADER_NAME = "Authorization";

    /**
     * Given a rest request it will extract authentication token
     * 
     * If no token was found, returns null. 
     */
    public static AuthToken extractToken(final RestRequest request) {

        // Extract authentication information from headers
        final Optional<String> authHeaderValue = request.getHeaders()
            .getOrDefault(AUTH_HEADER_NAME, Collections.emptyList())
            .stream()
            .findFirst();

        if (authHeaderValue.isPresent()) {
            final String authHeaderValueStr = authHeaderValue.get();

            if (authHeaderValueStr.startsWith(BasicAuthToken.TOKEN_IDENIFIER)) {
                return new BasicAuthToken(authHeaderValueStr);
            } else {
                throw new UnsupportedHttpAuthenticationToken(authHeaderValueStr);
            }
        }

        // No token found
        return null;
    }
} 