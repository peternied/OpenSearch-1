/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.identity.tokens;

/**
 * Basic (Base64 encoded) Authentication Token in a http request header
 */
public final class BasicAuthToken implements AuthToken {

    public final static String HEADER_NAME = "Authorization";
    
    private String headerValue;

    public BasicAuthToken(String headerValue) {
        this.headerValue = headerValue;
    }

    public String getHeaderValue() {
        return headerValue;
    }
}
