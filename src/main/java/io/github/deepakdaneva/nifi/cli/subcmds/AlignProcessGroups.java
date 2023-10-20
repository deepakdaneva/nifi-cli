/*
 * Copyright (C) 2023 Deepak Kumar Jangir
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
package io.github.deepakdaneva.nifi.cli.subcmds;

import io.github.deepakdaneva.nifi.cli.services.NiFiService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.apache.nifi.web.api.dto.PositionDTO;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.nifi.web.api.dto.flow.FlowDTO;
import org.apache.nifi.web.api.entity.ProcessGroupEntity;
import org.apache.nifi.web.api.entity.ProcessGroupFlowEntity;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
@CommandLine.Command(name = "align", description = "Align independent process groups on the canvas in a grid manner.")
public class AlignProcessGroups implements Runnable {

    /**
     * Fixed width of the process group component on the flow canvas
     */
    static final int PG_WIDTH = 384;
    /**
     * Fixed height of the process group component on the flow canvas
     */
    static final int PG_HEIGHT = 176;
    /**
     * Fixed gap between the aligned process groups
     */
    static final int PG_GAP = 10;
    /**
     * Command Spec
     */
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;
    /**
     * Depth of the process groups to align process groups upto
     */
    @CommandLine.Option(names = {Options.D, Options.DEPTH}, defaultValue = "5", description = "Depth upto which process groups needs to be aligned. NOTE: Provide -1 to align all process groups recursively.")
    int givenDepth;
    /**
     * NiFi Root process group id
     */
    @CommandLine.Option(names = {Options.R, Options.ROOTPGID}, description = "Root process group id from which aligning the process groups should be started down the hierarchy. NOTE: If not provided then root process group i.e. 'NIFI Flow' will be used.")
    String givenRootPgId;
    /**
     * Maximum number of columns to align upto
     */
    int PG_GRID_MAX_COLUMNS;
    /**
     * NiFi Service
     */
    @Inject
    @RestClient
    NiFiService nifiService;

    /**
     * Actual command logic to execute
     */
    @Override
    public void run() {
        givenRootPgId = givenRootPgId != null ? givenRootPgId.trim() : "";
        if (givenDepth != 0) {
            ProcessGroupFlowEntity pgfDto;
            if (givenRootPgId.isEmpty()) {
                pgfDto = nifiService.getFlowProcessGroup("root");
            } else {
                pgfDto = nifiService.getFlowProcessGroup(givenRootPgId);
            }
            Log.info("Aligning Process Groups...");
            align(givenDepth, pgfDto);
            Log.info("Aligning Completed!");
        }
    }

    /**
     * Align all the independent process groups
     * 
     * @param depth upto which process groups should be aligned
     * @param pgfe process group flow entity instance
     */
    private void align(int depth, ProcessGroupFlowEntity pgfe) {
        if (depth > 0) {
            if (pgfe != null) {
                FlowDTO flow = pgfe.getProcessGroupFlow().getFlow();
                if (flow.getConnections().isEmpty() && flow.getFunnels().isEmpty() && flow.getInputPorts().isEmpty() && flow.getOutputPorts().isEmpty() && flow.getRemoteProcessGroups().isEmpty() && flow.getProcessors().isEmpty()) {
                    if (!flow.getProcessGroups().isEmpty()) {
                        int currentRow = 0;
                        int currentColumn = 0;
                        for (ProcessGroupEntity pgEntity : flow.getProcessGroups()) {
                            try {
                                String currentPgId = pgEntity.getId();
                                ProcessGroupFlowEntity pgFlowEntity = nifiService.getFlowProcessGroup(currentPgId);
                                // align sub process groups first
                                if (depth - 1 != 0) {
                                    align(depth - 1, pgFlowEntity);
                                }
                                // align current pg
                                ProcessGroupEntity newPgEntity = new ProcessGroupEntity();
                                newPgEntity.setRevision(pgEntity.getRevision());

                                ProcessGroupDTO pgDto = new ProcessGroupDTO();
                                pgDto.setId(currentPgId);
                                pgDto.setPosition(getPos(currentRow, currentColumn));

                                newPgEntity.setComponent(pgDto);
                                nifiService.updateProcessGroup(currentPgId, newPgEntity);
                            } catch (Exception e) {
                                Log.error("Unable to align process groups: " + e.getMessage());
                                System.exit(CommandLine.ExitCode.SOFTWARE);
                            }
                            currentColumn++;
                            if (currentColumn == PG_GRID_MAX_COLUMNS) {
                                currentColumn = 0;
                                currentRow++;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Provides the new instance of {@link org.apache.nifi.web.api.dto.PositionDTO} by calculating using current row and current column
     * 
     * @param currentRow current row of the grid
     * @param currentColumn current column of the grid
     * @return position of the component to align
     */
    private PositionDTO getPos(int currentRow, int currentColumn) {
        return new PositionDTO((double) (currentColumn * (PG_WIDTH + PG_GAP)), (double) (currentRow * (PG_HEIGHT + PG_GAP)));
    }

    /**
     * Sets the ${@link io.github.deepakdaneva.nifi.cli.subcmds.AlignProcessGroups#PG_GRID_MAX_COLUMNS} by using the provided maximum column number string by the user.
     * 
     * @param value string maximum columns
     * @throws Exception if provided maximum column number is not valid
     */
    @CommandLine.Option(names = {Options.C, Options.COLUMNS}, defaultValue = "4", description = "Maximum number of columns. NOTE: This should not be less than 1.")
    void setColumns(String value) throws Exception {
        try {
            PG_GRID_MAX_COLUMNS = Integer.parseInt(value);
            if (PG_GRID_MAX_COLUMNS < 1) {
                throw new IllegalArgumentException("Maximum columns number can not be less than 1.");
            }
        } catch (NumberFormatException nfe) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Invalid columns number (" + value + ") provided.");
        } catch (IllegalArgumentException iae) {
            throw new CommandLine.ParameterException(spec.commandLine(), iae.getMessage());
        }
    }

    /**
     * Options for {@link io.github.deepakdaneva.nifi.cli.subcmds.AlignProcessGroups}
     */
    public static final class Options {
        /**
         * Depth of the process groups
         */
        public static final String D = "-d";
        /**
         * Depth of the process groups
         */
        public static final String DEPTH = "--depth";
        /**
         * Root process group id
         */
        public static final String R = "-r";
        /**
         * Root process group id
         */
        public static final String ROOTPGID = "--rootpgid";
        /**
         * Grid columns to align process groups into
         */
        public static final String C = "-c";
        /**
         * Grid columns to align process groups into
         */
        public static final String COLUMNS = "--columns";
    }

}
