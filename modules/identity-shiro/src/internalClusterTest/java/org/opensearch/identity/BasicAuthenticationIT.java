/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.shiro;

import org.opensearch.action.admin.cluster.node.info.NodeInfo;
import org.opensearch.action.admin.cluster.state.ClusterStateResponse;
import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.opensearch.test.OpenSearchIntegTestCase;
import org.opensearch.test.OpenSearchIntegTestCase.ClusterScope;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertNoTimeout;

@ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 0)
public class BasicAuthenticationIT extends HttpSmokeTestCaseWithIdentity {

    public void testBasicAuth() throws Exception {
        final String clusterManagerNode = internalCluster().startClusterManagerOnlyNode();

        ClusterStateResponse clusterStateResponse = client(clusterManagerNode).admin()
            .cluster()
            .prepareState()
            .setClusterManagerNodeTimeout("1s")
            .clear()
            .setNodes(true)
            .get();
        assertNotNull(clusterStateResponse.getState().nodes().getClusterManagerNodeId());

        // start another node
        final String dataNode = internalCluster().startDataOnlyNode();
        clusterStateResponse = client(dataNode).admin()
            .cluster()
            .prepareState()
            .setClusterManagerNodeTimeout("1s")
            .clear()
            .setNodes(true)
            .setLocal(true)
            .get();
        assertNotNull(clusterStateResponse.getState().nodes().getClusterManagerNodeId());
        // wait for the cluster to form
        assertNoTimeout(client().admin().cluster().prepareHealth().setWaitForNodes(Integer.toString(2)).get());
        List<NodeInfo> nodeInfos = client().admin().cluster().prepareNodesInfo().get().getNodes();
        assertEquals(2, nodeInfos.size());

        Request request = new Request("GET", "/_cluster/health");
        RequestOptions options = RequestOptions.DEFAULT.toBuilder().addHeader("Authorization", "Basic YWRtaW46YWRtaW4=").build(); // admin:admin
        request.setOptions(options);
        List<NodeInfo> dataNodeInfos = nodeInfos.stream().filter(ni -> ni.getNode().isDataNode()).collect(Collectors.toList());
        RestClient restClient = createRestClient(dataNodeInfos, null, "http");

        Response response = restClient.performRequest(request);
        String content = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertTrue(content.contains("\"status\":\"green\""));
    }
}
