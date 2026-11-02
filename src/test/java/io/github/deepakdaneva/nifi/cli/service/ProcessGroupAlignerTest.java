/*
 * Copyright (C) 2026 Deepak Kumar Jangir
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.github.deepakdaneva.nifi.cli.service;

import io.github.deepakdaneva.nifi.cli.client.NiFiClient;
import io.github.deepakdaneva.nifi.cli.exceptions.NiFiResponseException;
import org.apache.nifi.web.api.dto.PositionDTO;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.nifi.web.api.dto.RevisionDTO;
import org.apache.nifi.web.api.dto.flow.FlowDTO;
import org.apache.nifi.web.api.dto.flow.ProcessGroupFlowDTO;
import org.apache.nifi.web.api.entity.ProcessGroupEntity;
import org.apache.nifi.web.api.entity.ProcessGroupFlowEntity;
import org.apache.nifi.web.api.entity.ProcessorEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessGroupAlignerTest {

    private final ProcessGroupAligner aligner = new ProcessGroupAligner();

    @Test
    void positionsFollowGridGeometry() {
        assertPosition(0, 0, ProcessGroupAligner.position(0, 0));
        assertPosition(394, 0, ProcessGroupAligner.position(0, 1));
        assertPosition(788, 372, ProcessGroupAligner.position(2, 2));
    }

    @Test
    void alignsIndependentGroupsSortedByNameIntoColumns() {
        FakeNiFi nifi = new FakeNiFi();
        nifi.flow("root", group("pg-c", "charlie", 1), group("pg-a", "Alpha", 2), group("pg-b", "bravo", 3));
        nifi.flow("pg-a");
        nifi.flow("pg-b");
        nifi.flow("pg-c");

        int moved = aligner.align(nifi, null, 1, 2);

        assertEquals(3, moved);
        assertEquals(List.of("pg-a", "pg-b", "pg-c"), new ArrayList<>(nifi.updates.keySet()));
        assertPosition(0, 0, nifi.updates.get("pg-a").getComponent().getPosition());
        assertPosition(394, 0, nifi.updates.get("pg-b").getComponent().getPosition());
        assertPosition(0, 186, nifi.updates.get("pg-c").getComponent().getPosition());
        assertEquals(3L, nifi.updates.get("pg-b").getRevision().getVersion());
        assertEquals("pg-b", nifi.updates.get("pg-b").getComponent().getId());
        // depth 1 must not descend into children
        assertEquals(List.of("root"), nifi.fetched);
    }

    @Test
    void recursesUpToDepth() {
        FakeNiFi nifi = threeLevels();
        assertEquals(2, aligner.align(nifi, "", 2, 4));
        assertEquals(Set.of("l1", "l2"), nifi.updates.keySet());
    }

    @Test
    void negativeDepthAlignsAllLevels() {
        FakeNiFi nifi = threeLevels();
        assertEquals(3, aligner.align(nifi, null, -1, 4));
        assertEquals(Set.of("l1", "l2", "l3"), nifi.updates.keySet());
        // children are aligned before their parent
        assertEquals(List.of("l3", "l2", "l1"), new ArrayList<>(nifi.updates.keySet()));
    }

    @Test
    void zeroDepthDoesNothing() {
        FakeNiFi nifi = threeLevels();
        assertEquals(0, aligner.align(nifi, null, 0, 4));
        assertTrue(nifi.fetched.isEmpty());
    }

    @Test
    void startsFromGivenProcessGroup() {
        FakeNiFi nifi = threeLevels();
        assertEquals(1, aligner.align(nifi, " l1 ", 1, 4));
        assertEquals(List.of("l1"), nifi.fetched);
        assertEquals(Set.of("l2"), nifi.updates.keySet());
    }

    @Test
    void leavesCanvasWithOtherComponentsUntouched() {
        FakeNiFi nifi = new FakeNiFi();
        ProcessGroupFlowEntity root = nifi.flow("root", group("pg-a", "a", 1));
        root.getProcessGroupFlow().getFlow().setProcessors(Set.of(new ProcessorEntity()));
        assertEquals(0, aligner.align(nifi, null, -1, 4));
        assertTrue(nifi.updates.isEmpty());
    }

    @Test
    void toleratesMissingCollections() {
        FakeNiFi nifi = new FakeNiFi();
        ProcessGroupFlowEntity root = nifi.flow("root", group("pg-a", "a", 1));
        FlowDTO flow = root.getProcessGroupFlow().getFlow();
        flow.setConnections(null);
        flow.setFunnels(null);
        nifi.flow("pg-a").getProcessGroupFlow().setFlow(null);
        assertEquals(1, aligner.align(nifi, null, 3, 4));
    }

    @Test
    void rejectsInvalidColumns() {
        assertThrows(IllegalArgumentException.class, () -> aligner.align(new FakeNiFi(), null, 1, 0));
    }

    @Test
    void propagatesNiFiErrors() {
        FakeNiFi nifi = new FakeNiFi();
        nifi.flow("root", group("pg-a", "a", 1));
        nifi.failUpdates = true;
        NiFiResponseException e = assertThrows(NiFiResponseException.class, () -> aligner.align(nifi, null, 1, 4));
        assertEquals(409, e.getStatus());
    }

    private static FakeNiFi threeLevels() {
        FakeNiFi nifi = new FakeNiFi();
        nifi.flow("root", group("l1", "l1", 0));
        nifi.flow("l1", group("l2", "l2", 0));
        nifi.flow("l2", group("l3", "l3", 0));
        nifi.flow("l3");
        return nifi;
    }

    private static void assertPosition(double x, double y, PositionDTO position) {
        assertEquals(x, position.getX());
        assertEquals(y, position.getY());
    }

    private static ProcessGroupEntity group(String id, String name, long version) {
        ProcessGroupDTO component = new ProcessGroupDTO();
        component.setId(id);
        component.setName(name);
        component.setPosition(new PositionDTO(1234.0, 5678.0));
        RevisionDTO revision = new RevisionDTO();
        revision.setVersion(version);
        ProcessGroupEntity entity = new ProcessGroupEntity();
        entity.setId(id);
        entity.setRevision(revision);
        entity.setComponent(component);
        return entity;
    }

    /**
     * In-memory stand-in for the NiFi REST API.
     */
    private static final class FakeNiFi implements NiFiClient {
        private final Map<String, ProcessGroupFlowEntity> flows = new HashMap<>();
        private final Map<String, ProcessGroupEntity> updates = new LinkedHashMap<>();
        private final List<String> fetched = new ArrayList<>();
        private boolean failUpdates;

        ProcessGroupFlowEntity flow(String id, ProcessGroupEntity... children) {
            FlowDTO flow = new FlowDTO();
            flow.setProcessGroups(new LinkedHashSet<>(List.of(children)));
            flow.setProcessors(Set.of());
            flow.setConnections(Set.of());
            flow.setFunnels(Set.of());
            flow.setInputPorts(Set.of());
            flow.setOutputPorts(Set.of());
            flow.setRemoteProcessGroups(Set.of());
            ProcessGroupFlowDTO pgFlow = new ProcessGroupFlowDTO();
            pgFlow.setId(id);
            pgFlow.setFlow(flow);
            ProcessGroupFlowEntity entity = new ProcessGroupFlowEntity();
            entity.setProcessGroupFlow(pgFlow);
            flows.put(id, entity);
            return entity;
        }

        @Override
        public String getAccessToken(String username, String password) {
            return "token";
        }

        @Override
        public void logout() {
        }

        @Override
        public ProcessGroupFlowEntity getFlowProcessGroup(String id) {
            fetched.add(id);
            ProcessGroupFlowEntity entity = flows.get(id);
            if (entity == null) {
                throw new NiFiResponseException("Unable to find process group with id '" + id + "'.", 404);
            }
            return entity;
        }

        @Override
        public void updateProcessGroup(String id, ProcessGroupEntity pgEntity) {
            if (failUpdates) {
                throw new NiFiResponseException("Revision mismatch", 409);
            }
            updates.put(id, pgEntity);
        }
    }
}
