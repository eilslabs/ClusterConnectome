/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs.cluster.pbs

import de.dkfz.roddy.BEException
import de.dkfz.roddy.config.JobLog
import de.dkfz.roddy.execution.jobs.BEJob
import de.dkfz.roddy.execution.jobs.BEJobID
import de.dkfz.roddy.execution.jobs.ProcessingParameters
import de.dkfz.roddy.execution.jobs.SubmissionCommand
import de.dkfz.roddy.execution.jobs.SubmissionCommand.PassEnvironmentVariables as PassVars
import de.dkfz.roddy.tools.LoggerWrapper

import java.util.logging.Level

import static de.dkfz.roddy.StringConstants.COLON

/**
 * This class is used to create and execute qsub commands
 *
 * @author michael
 */
@groovy.transform.CompileStatic
class PBSCommand extends SubmissionCommand {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(PBSCommand.class.name)

    public static final String NONE = "none"
    public static final String AFTEROK = "afterok"
    public static final String PARM_DEPENDS = " -W depend="

    /**
     * The command which should be called
     */
    protected String command

    protected List<String> dependencyIDs

    protected final List<ProcessingParameters> processingParameters

    /**
     *
     * @param name
     * @param parameters
     * @param command
     * @param filesToCheck
     */
    PBSCommand(PBSJobManager parentManager, BEJob job, String name, List<ProcessingParameters> processingParameters, Map<String, String> parameters, List<String> dependencyIDs, String command) {
        super(parentManager, job, name, parameters)
        this.processingParameters = processingParameters
        this.command = command
        this.dependencyIDs = dependencyIDs ?: new LinkedList<String>()
    }

    @Override
    protected String getJobNameParameter() {
        "-N ${jobName}" as String
    }

    @Override
    protected String getHoldParameter() {
        return "-h"
    }

    @Override
    protected String getAccountParameter(String account) {
        return account ? "-A ${account}" as String : ""
    }

    @Override
    protected String getWorkingDirectory() {
        return "-w ${job.getWorkingDirectory() ?: WORKING_DIRECTORY_DEFAULT}" as String
    }

    @Override
    protected String getLoggingParameter(JobLog jobLog) {
        if (!jobLog.out && !jobLog.error) {
            return "-k"
        } else if (jobLog.out == jobLog.error) {
            return "${joinLogParameter} -o \"${jobLog.out.replace(JobLog.JOB_ID, '\\$PBS_JOBID')}\""
        } else {
            return "-o \"${jobLog.out.replace(JobLog.JOB_ID, '\\$PBS_JOBID')} -e ${jobLog.error.replace(JobLog.JOB_ID, '\\$PBS_JOBID')}\""
        }
    }

    protected String getJoinLogParameter() {
        return "-j oe"
    }

    @Override
    protected String getEmailParameter(String address) {
        return  address ? " -M " + address : ""
    }

    @Override
    protected String getGroupListParameter(String groupList) {
        return " -W group_list="+ groupList
    }

    @Override
    protected String getUmaskString(String umask) {
        return umask ? "-W umask=" + umask : ""
    }

    @Override
    String assembleDependencyString(List<BEJobID> jobIds) {
        StringBuilder qsubCall = new StringBuilder("")
        LinkedList<String> tempDependencies =
                jobIds.findAll {
                    it.getId() != "" && it.getId() != NONE && it.getId() != "-1"
                }.collect {
                    it.getId().split("\\.")[0] // Keep the command line short. PBS accepts the job number for dependencies.
                } as LinkedList<String>
        if (tempDependencies.size() > 0) {
            try {
                qsubCall <<
                        getDependsSuperParameter() <<
                        getDependencyParameterName() <<
                        getDependencyOptionSeparator() <<
                        tempDependencies.join(getDependencyIDSeparator())
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.toString())
            }
        }

        return qsubCall
    }

    @Override
    String assembleVariableExportParameters() {
        List<String> parameterStrings = []

        if (passLocalEnvironment == PassVars.None && !parameters.isEmpty())
            throw new BEException("passLocalEnvironment is set to 'None' but you still request the passing of variables: ${parameters}")
        else if (passLocalEnvironment == PassVars.All) {
            parameterStrings << "-V"
        }

        List<String> environmentStrings = []

        if (job.parameters.containsKey("CONFIG_FILE") && job.parameters.containsKey("PARAMETER_FILE")) {
            // TODO: Clarify relation between Job and Command w.r.t. parameters. Then get rid of Roddy-specific code here.
            // This code is exclusively meant to quickfix Roddy. Remove this branch if Roddy is fixed.
            // WARNING: Note the additional space before the parameter delimiter! It is necessary for bsub -env but must not be there for qsub in PBS!
            environmentStrings << "CONFIG_FILE=" + job.parameters["CONFIG_FILE"]
            environmentStrings << "PARAMETER_FILE=" + job.parameters["PARAMETER_FILE"]

            if (job.parameters.containsKey("debugWrapInScript")) {
                environmentStrings << "debugWrapInScript=" + job.parameters["debugWrapInScript"]
            }
        }

        environmentStrings += parameters.collect { key, value ->
            if (null == value)
                key                   // returning just the variable name make bsub take the value form the *bsub-commands* execution environment
            else
                "${key}=${value}"     // sets value to value
        } as List

        if (environmentStrings.empty)
            return parameterStrings
        else {
            parameterStrings << "-v \"" + environmentStrings.join(",") + "\""
            return parameterStrings.join(" ")
        }
    }

    String getDependencyParameterName() {
        return AFTEROK
    }

    /**
     * In this case i.e. afterokarray:...,afterok:
     * A comma
     * @return
     */

    protected String getDependencyOptionSeparator() {
        return COLON
    }

    protected String getDependencyIDSeparator() {
        return COLON
    }

    protected String getAdditionalCommandParameters() {
        return ""
    }

    protected String getDependsSuperParameter() {
        PARM_DEPENDS
    }
}
