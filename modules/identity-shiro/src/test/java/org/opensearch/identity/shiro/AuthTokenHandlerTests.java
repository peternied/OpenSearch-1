/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.shiro;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.opensearch.identity.tokens.BasicAuthToken;
import org.opensearch.test.OpenSearchTestCase;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AuthTokenHandlerTests extends OpenSearchTestCase {

    public void testShouldExtractBasicAuthTokenSuccessfully() {
        final BasicAuthToken authToken = new BasicAuthToken("Basic YWRtaW46YWRtaW4="); // admin:admin

        final AuthenticationToken translatedToken = AuthTokenHandler.translateAuthToken(authToken);
        assertThat(translatedToken, is(instanceOf(UsernamePasswordToken.class)));

        final UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) translatedToken;
        assertThat(usernamePasswordToken, notNullValue());
        assertThat(usernamePasswordToken.getUsername(), equalTo("admin"));
        assertThat(new String(usernamePasswordToken.getPassword()), equalTo("admin"));
    }

    public void testShouldExtractBasicAuthTokenSuccessfully_twoSemiColonPassword() {
        final BasicAuthToken authToken = new BasicAuthToken("Basic dGVzdDp0ZTpzdA=="); // test:te:st

        final AuthenticationToken translatedToken = AuthTokenHandler.translateAuthToken(authToken);
        assertThat(translatedToken, is(instanceOf(UsernamePasswordToken.class)));

        final UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) translatedToken;
        assertThat(usernamePasswordToken, notNullValue());
        assertThat(usernamePasswordToken.getUsername(), equalTo("test"));
        assertThat(new String(usernamePasswordToken.getPassword()), equalTo("te:st"));
    }

    public void testShouldReturnNullWhenExtractingNullToken() {
        final AuthenticationToken translatedToken = AuthTokenHandler.translateAuthToken(null);

        assertThat(translatedToken, nullValue());
    }
}
