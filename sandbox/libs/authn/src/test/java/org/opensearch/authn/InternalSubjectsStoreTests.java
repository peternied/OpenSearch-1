/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.authn;

import org.opensearch.authn.realm.InternalSubjectsStore;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Map;

public class InternalSubjectsStoreTests extends OpenSearchTestCase {

    public void testReadInternalSubjectsTest() {
        String internalSubjectsYaml = "internal_users_test.yml";
        Map<String, InternalSubject> internalSubjectMap = InternalSubjectsStore.readInternalSubjectsAsMap(internalSubjectsYaml);
        assertTrue(internalSubjectMap.containsKey("new-user"));
        assertFalse(internalSubjectMap.containsKey("new-user2"));
    }

    public void testReadInternalSubjectsFileDoesNotExistTest() {
        String internalSubjectsYaml = "config/does_not_exist.yml";
        try {
            Map<String, InternalSubject> internalSubjectMap = InternalSubjectsStore.readInternalSubjectsAsMap(internalSubjectsYaml);
            fail("Expected to throw FileNotFoundException");
        } catch (RuntimeException e) {
            // expected
        }
    }
}
