/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.shiro;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.opensearch.common.util.set.Sets;
import org.opensearch.identity.Scope;
import org.opensearch.identity.Subject;
import org.opensearch.identity.tokens.AuthToken;

/**
 * Subject backed by Shiro
 *
 * @opensearch.experimental
 */
public class ShiroSubject implements Subject {
    private final AuthTokenHandler authTokenHandler;
    private final org.apache.shiro.subject.Subject shiroSubject;
    private List<Scope> scopes;

    /**
     * Creates a new shiro subject for use with the IdentityPlugin
     * Cannot return null
     * @param authTokenHandler Used to extract auth header info
     * @param subject The specific subject being authc/z'd
     */
    public ShiroSubject(final AuthTokenHandler authTokenHandler, final org.apache.shiro.subject.Subject subject) {
        this.authTokenHandler = Objects.requireNonNull(authTokenHandler);
        this.shiroSubject = Objects.requireNonNull(subject);
        this.scopes = List.of();
    }

    /**
     * Return the current principal
     *
     * @return The current principal
     */
    @Override
    public Principal getPrincipal() {
        final Object o = shiroSubject.getPrincipal();
        if (o == null) return null;
        if (o instanceof Principal) return (Principal) o;
        return () -> o.toString();
    }

    /**
     * Check if another object is equal to this object
     *
     * @param obj The object to be compared against this
     * @return Whether the two objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final Subject that = (Subject) obj;
        return Objects.equals(getPrincipal(), that.getPrincipal());
    }

    /**
     * Return this Subject's principal as a hash
     * @return An int hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(getPrincipal());
    }

    /**
     * Convert this ShiroSubject's principal to a string
     * @return A string of the subject's principal
     */
    @Override
    public String toString() {
        return "ShiroSubject(principal=" + getPrincipal() + ")";
    }

    /**
     * Logs the user in via authenticating the user against current Shiro realm
     * @param authenticationToken The authToken to be used for login
     */
    public void authenticate(AuthToken authenticationToken) {
        final org.apache.shiro.authc.AuthenticationToken authToken = authTokenHandler.translateAuthToken(authenticationToken)
            .orElseThrow(() -> new UnsupportedAuthenticationToken());
        shiroSubject.login(authToken);
    }

    /**
     * Sets the scopes of the Subject to the provided list
     * @param scopes The scopes the subject should have
     */
    public void setScopes(List<Scope> scopes) {
        this.scopes = (scopes);
    }

    /**
     * @return The scopes associated with the subject
     */
    public List<Scope> getScopes() {
        return this.scopes;
    }

    /**
     * @param scope The scope to check against the subject's associated scopes
     * @return Whether any of the scopes match
     */
    @Override
    public boolean isAllowed(List<Scope> scope) {
        Set<Scope> intersection = Sets.intersection(Set.copyOf(this.scopes), Set.copyOf(scope));
        return intersection.size() > 0;
    }
}
