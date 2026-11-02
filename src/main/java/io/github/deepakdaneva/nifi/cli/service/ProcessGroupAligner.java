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
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.nifi.web.api.dto.PositionDTO;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.nifi.web.api.dto.flow.FlowDTO;
import org.apache.nifi.web.api.dto.flow.ProcessGroupFlowDTO;
import org.apache.nifi.web.api.entity.ProcessGroupEntity;
import org.apache.nifi.web.api.entity.ProcessGroupFlowEntity;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Arranges process groups on the NiFi canvas in a grid.
 * <p>
 * Only "independent" process groups are moved: a canvas is rearranged only if it contains nothing but process groups (no processors,
 * connections, ports, funnels or remote process groups), so that hand-made layouts of real flows are never disturbed. Process groups are placed
 * row by row, ordered by name (case-insensitive) and then id, so repeated runs give the same layout.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 2
 */
@ApplicationScoped
public class ProcessGroupAligner {

    /**
     * Process group id that NiFi resolves to the root process group.
     */
    public static final String ROOT_PROCESS_GROUP_ID = "root";
    /**
     * Width of a process group component on the canvas.
     */
    public static final int PG_WIDTH = 384;
    /**
     * Height of a process group component on the canvas.
     */
    public static final int PG_HEIGHT = 176;
    /**
     * Gap between aligned process groups.
     */
    public static final int PG_GAP = 10;

    private static final Comparator<ProcessGroupEntity> ORDER = Comparator.comparing(ProcessGroupAligner::sortName).thenComparing(ProcessGroupEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * Creates the aligner.
     */
    public ProcessGroupAligner() {
    }

    /**
     * Aligns the process groups below {@code rootPgId}.
     *
     * @param client authenticated NiFi client
     * @param rootPgId id of the process group whose children are aligned first; {@code null} or blank means the NiFi root process group
     * @param depth number of levels to align; {@code 0} does nothing, a negative value aligns all levels
     * @param columns maximum number of process groups per row, at least {@code 1}
     * @return number of process groups that were moved
     * @throws IllegalArgumentException if {@code columns} is less than 1
     */
    public int align(NiFiClient client, String rootPgId, int depth, int columns) {
        Objects.requireNonNull(client, "client");
        if (columns < 1) {
            throw new IllegalArgumentException("Maximum columns number can not be less than 1.");
        }
        if (depth == 0) {
            return 0;
        }
        String startId = rootPgId == null || rootPgId.isBlank() ? ROOT_PROCESS_GROUP_ID : rootPgId.trim();
        return alignLevel(client, client.getFlowProcessGroup(startId), depth, columns);
    }

    /**
     * Calculates the canvas position of the grid cell at {@code row}/{@code column}.
     *
     * @param row zero based row
     * @param column zero based column
     * @return position of the top left corner of the cell
     */
    public static PositionDTO position(int row, int column) {
        return new PositionDTO((double) (column * (PG_WIDTH + PG_GAP)), (double) (row * (PG_HEIGHT + PG_GAP)));
    }

    private int alignLevel(NiFiClient client, ProcessGroupFlowEntity flowEntity, int depth, int columns) {
        FlowDTO flow = flowOf(flowEntity);
        if (flow == null || !containsOnlyProcessGroups(flow)) {
            if (flow != null) {
                Log.debugf("Skipping process group %s: it contains components other than process groups", flowEntity.getProcessGroupFlow().getId());
            }
            return 0;
        }
        List<ProcessGroupEntity> groups = flow.getProcessGroups().stream().filter(Objects::nonNull).sorted(ORDER).toList();
        int moved = 0;
        int index = 0;
        for (ProcessGroupEntity group : groups) {
            String id = group.getId();
            if (depth < 0 || depth > 1) {
                moved += alignLevel(client, client.getFlowProcessGroup(id), depth < 0 ? depth : depth - 1, columns);
            }
            client.updateProcessGroup(id, positionUpdate(group, index / columns, index % columns));
            moved++;
            index++;
        }
        return moved;
    }

    private static ProcessGroupEntity positionUpdate(ProcessGroupEntity current, int row, int column) {
        ProcessGroupDTO component = new ProcessGroupDTO();
        component.setId(current.getId());
        component.setPosition(position(row, column));
        ProcessGroupEntity update = new ProcessGroupEntity();
        update.setRevision(current.getRevision());
        update.setComponent(component);
        return update;
    }

    private static FlowDTO flowOf(ProcessGroupFlowEntity entity) {
        if (entity == null) {
            return null;
        }
        ProcessGroupFlowDTO pgFlow = entity.getProcessGroupFlow();
        return pgFlow == null ? null : pgFlow.getFlow();
    }

    private static boolean containsOnlyProcessGroups(FlowDTO flow) {
        return isEmpty(flow.getConnections()) && isEmpty(flow.getFunnels()) && isEmpty(flow.getInputPorts()) && isEmpty(flow.getOutputPorts()) && isEmpty(flow.getRemoteProcessGroups()) && isEmpty(flow.getProcessors()) && !isEmpty(flow.getProcessGroups());
    }

    private static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    private static String sortName(ProcessGroupEntity entity) {
        ProcessGroupDTO component = entity.getComponent();
        String name = component == null ? null : component.getName();
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
