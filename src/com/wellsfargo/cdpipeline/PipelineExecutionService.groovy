package com.wellsfargo.cdpipeline

import com.wellsfargo.cdpipeline.buildWithParameters.BuildParameters
import com.wellsfargo.cdpipeline.checkout.PostCheckout
import com.wellsfargo.cdpipeline.spe.SecurityPolicyEngine
import com.wellsfargo.cdpipeline.checkout.CheckoutImpl
import com.wellsfargo.cdpipeline.discover.SharedLibrary
import com.wellsfargo.cdpipeline.notify.EmailNotification
import com.wellsfargo.cdpipeline.parser.YamlPipelineParser
import com.wellsfargo.cdpipeline.util.ExceptionUtil
import com.wellsfargo.cdpipeline.util.log.Logger
import com.wellsfargo.cdpipeline.buildpack.BuildPackBuilder
import com.wellsfargo.cdpipeline.util.git.CommitterInfo
import com.wellsfargo.cdpipeline.buildWithParameters.SetupBuildParameters
import com.wellsfargo.cdpipeline.jenkins.pipeline.steps.StepExecutor
import com. wellsfargo.cdpipeline.util.node.workerDetails
import com.wellsfargo.cdpipeline.build.BuildConfig

Class PipelineExecutionService {

    def wfs
	Logger logger
	
	PipelineExecutionService(def wfs) {
	     this.wfs = wfs
		 this.wfs.println "Invoked PipelineExecutionService!"
		 logger = new Logger(new)
	}
	
	def execute() {
    	//Default LOG_LEVEL, this id needed since CICD.yml isn't loaded till later and some steps
	    // are calling logger but arent capture due to non existing default
	logger.setLevel("INFO")
	
	wfs.timestamps {
	  wfs.ansiColor('xterm') {
	     def SharedLibraryName = wfs.env.SharedLibraryName
		 wfs.CurrentBuild.result = "SUCCESS"
		 def jenkinsSubDomain = "${wfs.env.JENKINS_URL}".split("//")[1].split("\\.")[0]
		 def jobName = "${wfs.env.JOB_NAME}".replaceAll("[\\W]","_")
		 def workSpacePath = "workspace/${jenkinsSubDomain}/${jobName.hashcode()}"+"/${wfs.env.BUILD_NUMBER}"
		 def projectYamlPipelineDefinition, cdPipelineYamlConfig
		 PipelineConfig.instance.workSpacePath = workSpacePath
		 PipelineConfig.instance.workerLabel = wfs.env.agent
		 
		 try {
		      wfs.node(PipelineConfig.instance.workerLabel) {
			      new WorkerDetails(wfs).setLoctionLabel()
				  if (!wfs.isUnix()) {
				       PipelineConfig.instance.workerLabel = wfs.env.NODE_NAME
					   BuildConfig.info("PipelineConfig.instance.workerLabel::" + PipelineConfig.instance.workerLabel)
				  }
				  
				  def cdPipelineYamlConfigString = wfs.libraryResource 'cdPipelineYamlConfig.yml'
				  cdPipelineYamlConfig = wfs.readYaml text: cdPipelineYamlConfigString
				  PipelineConfig.instance.pipelineAppConfig = cdPipelineYamlConfig
				  
				  def cdPipelineConstantYamlConfigString = wfs.libraryResource 'cdPipelineConstant.yml'
				  def cdPipelineConstantYamlConfig = wfs.readYaml text: cdPipelineConstantYamlConfigString
				  PipelineConfig.instance.pipelineConstant = cdPipelineConstantYamlConfig
				  
				  wfs.ws(PipelineConfig.instance.workSpacePath) {
				        boolean isCheckoutNeeded = true
						logger.debug("wfs.params.WORKSPACE_PATH: ${wfs.params.WORKSPACE_PATH}")
						if (wfs.params.WORKSPACE_PATH) {
						    isCheckoutNeeded = executeCopyScript()
							logger.info("isCheckoutNeeded:" + isCheckoutNeeded)
						}
						if (isCheckoutNeeded && wfs.params.TO_BUILDTYPE == "BLACKDUCK_DATA_COLLECTION"){
						    throw wfs.error("WorkSpace not found to copy")
						}
						
						def checkout = new CheckoutImpl(wfs)
						if (isCheckoutNeeded) {
						    checkout.gitCheckout()
						}
						
						if (buildIsTriggeredByCommitFromCI()) {
						    wfs.error('Build triggered from commit by CI system, aborting the build.")
						}
						
						/**
						* For regression testing we need to pass cicd.yml file as parameter
						* REGRESSION_TESTING and TEST_FILE_NAME are the two parameters passed as properties
						*/
						
						def cicdFileName = wfs.env.REGRESSION_TESTING ? wfs.env.TEST_FILE_NAME : "cicd.yml"
						
						if (wfs.fileExists("${wfs.env.WORKSPACE}/${cicdFileName}")) {
						    projectYamlPipelineDefinition = wfs.readYaml(file: "${wfs.env.WORKSPACE}/${cicdFileName}")
							PipelineConfig.instance.configFile = "cicd"
						} else if (wfs.fileExists("${wfs.env.WORKSPACE}/cd.yml")) {
						  wfs.println "reading cd file"
						  projectYamlPipelineDefinition = wfs.readYaml(file: "${wfs.env.WORKSPACE}/cd.yml")
						  PipelineConfig.instance.configFile = "cd"
						} else if (wfs.fileExists("${wfs.env.WORKSPACE}/ci.yml")) {
						  wfs.println "reading ci file"
						  projectYamlPipelineDefinition = wfs.readYaml(file: "${wfs.env.WORKSPACE}/ci.yml")
						  PipelineConfig.instance.configFile = "ci"
						}
						
						
				  
				  
				  
				  
				  
				  
				  
