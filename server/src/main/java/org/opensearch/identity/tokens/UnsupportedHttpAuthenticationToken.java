package org.opensearch.identity.tokens;

public class UnsupportedHttpAuthenticationToken extends RuntimeException {
    public UnsupportedHttpAuthenticationToken(final String unsupportedToken) {
        super("The following Authentication token is not supported, " + unsupportedToken);
    }
}