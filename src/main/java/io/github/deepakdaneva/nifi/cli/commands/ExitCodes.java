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
package io.github.deepakdaneva.nifi.cli.commands;

/**
 * Process exit codes of the CLI.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 2
 */
public final class ExitCodes {

    /**
     * The command completed successfully.
     */
    public static final int OK = 0;
    /**
     * The command failed, e.g. NiFi returned an error or could not be reached.
     */
    public static final int SOFTWARE = 1;
    /**
     * Invalid command line usage.
     */
    public static final int USAGE = 2;
    /**
     * NiFi rejected the credentials or does not support username/password login.
     */
    public static final int AUTHENTICATION = 3;

    /**
     * Usage help line for {@link #OK}.
     */
    public static final String LIST_OK = "0:Successful completion.";
    /**
     * Usage help line for {@link #SOFTWARE}.
     */
    public static final String LIST_SOFTWARE = "1:NiFi request failed or unexpected error.";
    /**
     * Usage help line for {@link #USAGE}.
     */
    public static final String LIST_USAGE = "2:Invalid command line input.";
    /**
     * Usage help line for {@link #AUTHENTICATION}.
     */
    public static final String LIST_AUTHENTICATION = "3:Authentication failed.";

    private ExitCodes() {
    }
}
