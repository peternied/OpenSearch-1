/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.shiro;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.hamcrest.MatcherAssert;
import org.opensearch.identity.tokens.AuthToken;
import org.opensearch.identity.tokens.BasicAuthToken;
import org.opensearch.test.OpenSearchTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AuthTokenHandlerTests extends OpenSearchTestCase {

    public void testShouldExtractBasicAuthTokenSuccessfully() {
        String authHeader = "Basic YWRtaW46YWRtaW4="; // admin:admin

        AuthToken authToken = new BasicAuthToken(authHeader);

        UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) AuthTokenHandler.translateAuthToken(authToken);

        MatcherAssert.assertThat(usernamePasswordToken, notNullValue());
        MatcherAssert.assertThat(usernamePasswordToken.getUsername(), equalTo("admin"));
        MatcherAssert.assertThat(new String(usernamePasswordToken.getPassword()), equalTo("admin"));
    }

    // TODO: Fix this test
    // public void testShouldExtractBasicAuthTokenSuccessfully_twoSemiColonPassword() {
    //     String authHeader = "Basic dGVzdDp0ZTpzdA=="; // test:te:st

    //     AuthToken authToken = new BasicAuthToken(authHeader);

    //     UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) AuthTokenHandler.translateAuthToken(authToken);

    //     MatcherAssert.assertThat(usernamePasswordToken, notNullValue());
    //     MatcherAssert.assertThat(usernamePasswordToken.getUsername(), equalTo("test"));
    //     MatcherAssert.assertThat(usernamePasswordToken.getPassword(), equalTo("te:st"));
    // }

    public void testShouldReturnNullWhenExtractingNullToken() {

        org.apache.shiro.authc.AuthenticationToken shiroAuthToken = AuthTokenHandler.translateAuthToken(null);

        MatcherAssert.assertThat(shiroAuthToken, nullValue());
    }
}
