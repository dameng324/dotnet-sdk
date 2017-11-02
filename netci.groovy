// Copyright (c) .NET Foundation and contributors. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

// Import the utility functionality.

import jobs.generation.ArchivalSettings;
import jobs.generation.Utilities;

def project = GithubProject
def branch = GithubBranchName
def isPR = true

def osList = ['Windows_NT', 'Windows_NT_FullFramework', 'Ubuntu14.04', 'Ubuntu16.04', 'OSX10.12']
def configList = ['Release', 'Debug']

def static getBuildJobName(def configuration, def os) {
    return configuration.toLowerCase() + '_' + os.toLowerCase()
}

osList.each { os ->
    configList.each { config ->
        // Calculate job name
        def jobName = getBuildJobName(config, os)
        def buildCommand = '';

        def osBase = os
        def machineAffinity = 'latest-or-auto'

        // Calculate the build command
        if (os == 'Windows_NT') {
            buildCommand = ".\\build\\cibuild.cmd -configuration $config"
            machineAffinity = 'latest-dev15-3-preview1'
        } else if (os == 'Windows_NT_FullFramework') {
            buildCommand = ".\\build\\cibuild.cmd -configuration $config -fullMSBuild"
            osBase = 'Windows_NT'
            machineAffinity = 'latest-dev15-3-preview1'
        } else {
            // Jenkins non-Ubuntu CI machines don't have docker
            buildCommand = "./build/cibuild.sh --configuration $config"
        }

        def newJob = job(Utilities.getFullJobName(project, jobName, isPR)) {
            // Set the label.
            steps {
                if (osBase == 'Windows_NT') {
                    // Batch
                    batchFile(buildCommand)
                }
                else {
                    // Shell
                    shell(buildCommand)
                }
            }
        }

        def archiveSettings = new ArchivalSettings()
        archiveSettings.addFiles("bin/**/*")
        archiveSettings.addFiles("bin/log/**/*")
        archiveSettings.excludeFiles("bin/obj/*")
        archiveSettings.setFailIfNothingArchived()
        archiveSettings.setArchiveOnFailure()
        Utilities.addArchival(newJob, archiveSettings)
        Utilities.setMachineAffinity(newJob, osBase, machineAffinity)
        Utilities.standardJobSetup(newJob, project, isPR, "*/${branch}")
        Utilities.addXUnitDotNETResults(newJob, "bin/$config/Tests/*TestResults.xml", false)
        Utilities.addGithubPRTriggerForBranch(newJob, branch, "$os $config")
    }
}
