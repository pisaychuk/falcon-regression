/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.falcon.regression.core.util;

import org.apache.falcon.regression.core.enumsAndConstants.MerlinConstants;
import org.apache.falcon.regression.core.response.graph.Edge;
import org.apache.falcon.regression.core.response.graph.EdgesResult;
import org.apache.falcon.regression.core.response.graph.GraphResult;
import org.apache.falcon.regression.core.response.graph.Vertex;
import org.apache.falcon.regression.core.response.graph.VerticesResult;
import org.apache.log4j.Logger;
import org.testng.Assert;

public class GraphAssert {
    private static Logger logger = Logger.getLogger(GraphAssert.class);

    private static void assertUserVertexAbsent(VerticesResult verticesResult) {
        for(Vertex vertex : verticesResult.getResults()) {
            if(vertex.getType().equals(Vertex.VERTEX_TYPE.USER.getValue())) {
                Assert.fail("Unexpected vertex for user is present: " + vertex);
            }
        }
    }

    public static void checkVerticesPresence(GraphResult graphResult, int minNumOfVertices) {
        Assert.assertTrue(graphResult.getTotalSize() >= minNumOfVertices,
            "graphResult should have at least " + minNumOfVertices + " vertex");
    }

    public static void assertVertexSanity(VerticesResult verticesResult) {
        Assert.assertEquals(verticesResult.getResults().length, verticesResult.getTotalSize(),
            "Size of vertices don't match");
        for (Vertex vertex : verticesResult.getResults()) {
            Assert.assertNotNull(vertex.get_id(),
                "id of the vertex should be non-null: " + vertex);
            Assert.assertNotNull(vertex.get_type(),
                "_type of the vertex should be non-null: " + vertex);
            Assert.assertNotNull(vertex.getName(),
                "name of the vertex should be non-null: " + vertex);
            Assert.assertNotNull(vertex.getType(),
                "id of the vertex should be non-null: " + vertex);
            Assert.assertNotNull(vertex.getTimestamp(),
                "id of the vertex should be non-null: " + vertex);
        }
    }

    public static void assertEdgeSanity(EdgesResult edgesResult) {
        for (Edge edge : edgesResult.getResults()) {
            Assert.assertNotNull(edge.get_id(), "id of an edge can't be null: " + edge);
            Assert.assertNotNull(edge.get_type(), "_type of an edge can't be null: " + edge);
            Assert.assertNotNull(edge.get_label(), "_label of an edge can't be null: " + edge);
            Assert.assertNotNull(edge.get_inV(), "_inV of an edge can't be null: " + edge);
            Assert.assertNotNull(edge.get_outV(), "_outV of an edge can't be null: " + edge);
        }
    }

    public static void assertUserVertexPresence(VerticesResult verticesResult) {
        checkVerticesPresence(verticesResult, 1);
        for(Vertex vertex : verticesResult.getResults()) {
            if(vertex.getType().equals(Vertex.VERTEX_TYPE.USER.getValue())) {
                if(vertex.getName().equals(MerlinConstants.CURRENT_USER_NAME)) {
                    return;
                }
            }
        }
        Assert.fail(String.format("Vertex corresponding to user: %s is not present.",
            MerlinConstants.CURRENT_USER_NAME));
    }

    private static void assertVerticesPresenceMinOccur(VerticesResult verticesResult,
                                                       Vertex.VERTEX_TYPE vertex_type,
                                                       final int minOccurrence) {
        int occurrence = 0;
        for(Vertex vertex : verticesResult.getResults()) {
            if(vertex.getType().equals(vertex_type)) {
                logger.info("Found vertex: " + vertex);
                occurrence++;
                if(occurrence >= minOccurrence) {
                    return;
                }
            }
        }
        Assert.fail(String.format("Expected at least %d vertices of type %s. But found only %d",
            minOccurrence, vertex_type, occurrence));
    }
}
