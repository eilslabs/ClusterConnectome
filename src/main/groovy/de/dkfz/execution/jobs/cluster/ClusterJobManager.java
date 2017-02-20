/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.execution.jobs.cluster;

import de.dkfz.execution.jobs.Command;
import de.dkfz.execution.jobs.JobManager;

/**
 * A class for processing backends running on a cluster.
 * This mainly defines variables and constants which can be set via the config.
 */
public abstract class ClusterJobManager<C extends Command> extends JobManager<C> {
    public static final String CVALUE_ENFORCE_SUBMISSION_TO_NODES="enforceSubmissionToNodes";
}
