package org.opensearch.identity;

import java.security.Principal;

/**
 * An individual, process, or device that causes information to flow among objects or change to the system state.
 */
public interface ISubject {
    /**
     * Get the principle associated with this Subject
     * */
    public Principal getPrincipal();

    /**
     * Check if the permission is allows for this subject
     *
     * @param permission The type of these expression of permissions is TDB
     * */
    public IPermissionResult isPermitted(final String permission);
}
