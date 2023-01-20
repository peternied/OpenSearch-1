/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

 package org.opensearch.identity.tokens;

/**
 * Exception when the http authentication token is not supported
 */
public class UnsupportedHttpAuthenticationToken extends RuntimeException {
    public UnsupportedHttpAuthenticationToken(final String unsupportedToken) {
        super("The following Authentication token is not supported, " + unsupportedToken);
    }
}