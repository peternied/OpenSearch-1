package org.opensearch.identity;

import java.security.Principal;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.List;
import java.util.Collections;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.opensearch.common.Strings;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.util.Base64;

/** Authentication manager using Shiro */
public class ShiroAuthenticationManager implements AuthenticationManager {

    private static final Logger LOGGER = LogManager.getLogger(ShiroAuthenticationManager.class);

    private final List<HeaderAuthorizer> authorizers;

    public ShiroAuthenticationManager() {
        // Ensure the shiro system configuration has been performed
        new MyShiroModule();

        // TODO: Dynamically configure authentication sources
        authorizers = Collections.singletonList(new ShiroBasicHeaderAuthorizer());
    }

    @Override
    public ISubject getCurrentSubject() {
        return new ShiroSubject(SecurityUtils.getSubject());
    }

    @Override
    public void authenticateWithHeader(final String authHeader) {
        // Iterate over the available authorizers to attempt to authorize the user
        for (final HeaderAuthorizer authorizer : authorizers) {
            if (authorizer.authorizeWithHeader(authHeader)) {
                return;
            }
        }
        throw new RuntimeException("Unable to authenticate user!");
    }

    @Override
    public AuthenticationSession dangerousAuthenticateAs(final String subject) {
        final org.apache.shiro.subject.Subject internalSubject = new org.apache.shiro.subject.Subject.Builder().authenticated(true)
            .principals(new SimplePrincipalCollection("INTERNAL-" + subject, "OpenSearch")) // How can we ensure the roles this
                                                                                            // princpal resolves?
            .contextAttribute("NodeId", "???") // Can we use this to source the originating node in a cluster?
            .buildSubject();

        final SubjectThreadState threadState = new SubjectThreadState(internalSubject);
        threadState.bind();

        return closeAuthenticateAsSession(threadState, subject);
    }

    private AuthenticationSession closeAuthenticateAsSession(final SubjectThreadState threadState, final String subjectReference) {
        return () -> {
            try {
                threadState.clear();
            } catch (final Exception e) {
                LOGGER.error("Unable to close authentication session as {}", subjectReference, e);
            }
        };
    }

    @Override
    public Runnable associateWith(final Runnable r) {
        return SecurityUtils.getSubject().associateWith(r);
    }

    @Override
    public <V> Callable<V> associateWith(final Callable<V> c) {
        return SecurityUtils.getSubject().associateWith(c);
    }

    @Override
    public void executeWith(final Runnable r) {
        SecurityUtils.getSubject().execute(r);
    }

    @Override
    public <V> V executeWith(final Callable<V> c) {
        return SecurityUtils.getSubject().execute(c);
    }

    /**
     * Wraps Shiro's Subject implementation
     */
    private static class ShiroSubject implements ISubject {

        private final org.apache.shiro.subject.Subject internalSubject;

        public ShiroSubject(org.apache.shiro.subject.Subject subject) {
            this.internalSubject = subject;
        }

        @Override
        public Principal getPrincipal() {
            final Object o = internalSubject.getPrincipal();

            if (o == null) {
                return null;
            }

            if (o instanceof Principal) {
                return (Principal) o;
            }

            return new Principal() {
                @Override
                public String getName() {
                    return o.toString();
                }
            };
        }

        @Override
        public IPermissionResult isPermitted(String permissionName) {
            final boolean isPermitted = internalSubject.isPermitted(permissionName);
            final Supplier<String> errorMessage = () -> "Unable to authorize " + this.toString() + ", for permission " + permissionName;
            return new PermissionResult(isPermitted, errorMessage);
        }

        @Override
        public String toString() {
            return this.getPrincipal().getName();
        }
    }

    private static class PermissionResult implements IPermissionResult {
        private final boolean isAllowed;
        private final Supplier<String> message;

        public PermissionResult(final boolean isAllowed, final Supplier<String> message) {
            this.isAllowed = isAllowed;
            this.message = message;
        }

        @Override
        public boolean isAllowed() {
            return isAllowed;
        }

        @Override
        public String getErrorMessage() {
            return message.get();
        }
    }

    /**
     * Authorizes a user from a HTTP Header
     */
    private interface HeaderAuthorizer {

        /**
         * Attempt to authorize the user
         */
        public boolean authorizeWithHeader(final String authHeader);
    }

    /**
     * THROW AWAY IMPLEMENTATION
     */
    private static class ShiroBasicHeaderAuthorizer implements HeaderAuthorizer {
        public boolean authorizeWithHeader(final String authHeader) {
            if (!(Strings.isNullOrEmpty(authHeader)) && authHeader.startsWith("Basic")) {
                final byte[] decodedAuthHeader = Base64.getDecoder().decode(authHeader.substring("Basic".length()).trim());
                final String[] decodedUserNamePassword = new String(decodedAuthHeader).split(":");
                final org.apache.shiro.subject.Subject currentSubject = SecurityUtils.getSubject();
                currentSubject.login(new UsernamePasswordToken(decodedUserNamePassword[0], decodedUserNamePassword[1]));
                LOGGER.info("Authenticated user '{}'", currentSubject.getPrincipal());
                return true;
            }
            return false;
        }
    }
}
