/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.shiro.realm;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.Before;
import org.opensearch.test.OpenSearchTestCase;

import java.io.FileNotFoundException;

public class InternalRealmTests extends OpenSearchTestCase {

    private InternalRealm realm;

    @Before
    public void setUpAndInitializeRealm() throws FileNotFoundException {
        realm = new InternalRealm.Builder("test").build();
    }

    public void testGetAuthenticationInfoUserExists() {
        String username = "admin";
        String password = "admin";
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        User admin = realm.getInternalUser("admin");
        AuthenticationInfo adminInfo = realm.getAuthenticationInfo(token);
        assertNotNull(adminInfo);
    }

    public void testGetAuthenticationInfoUserExistsWrongPassword() {
        String username = "admin";
        String password = "wrong_password";
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        User admin = realm.getInternalUser("admin");
        try {
            AuthenticationInfo adminInfo = realm.getAuthenticationInfo(token);
            fail("Expected to throw IncorrectCredentialsException");
        } catch (AuthenticationException e) {
            assertTrue(e instanceof IncorrectCredentialsException);
        }
    }
}
