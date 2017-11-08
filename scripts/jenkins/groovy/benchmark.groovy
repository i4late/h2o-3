def call(buildConfig, stageConfig) {

  def BENCHMARK_SUITE_DEFAULT_PATH = 'benchmarking_suite.csv'
  def H2O_3_BENCHMARK_HOME = 'h2o-3-benchmark'

  def insideDocker = load('h2o-3/scripts/jenkins/groovy/insideDocker.groovy')
  def buildTarget = load('h2o-3/scripts/jenkins/groovy/buildTarget.groovy')
  def customEnv = load('h2o-3/scripts/jenkins/groovy/customEnv.groovy')

  stage (stageConfig.stageName) {

    def currentRevision = null

    dir (H2O_3_BENCHMARK_HOME) {
      deleteDir()
      sh "git clone ${env.WORKSPACE}/h2o-3 ."
      sh 'git branch'
      currentRevision = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
    }

    writeFile file: BENCHMARK_SUITE_DEFAULT_PATH, text: """id,commit,num_of_runs,benchmark_file,env_file,bucket
1,${currentRevision},1,benchmark.sh,env-file,test.0xdata.com/h2o-3-benchmarks
"""
    writeFile file: 'env-file', text: ''
    writeFile file: 'benchmark.sh', text: """#! /bin/bash
echo Benchmark test
printenv
"""

    dir ('ml-benchmark') {
      checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'c6bab81a-6bb5-4497-9ec9-285ef5db36ea', url: 'https://github.com/h2oai/ml-benchmark']]]
    }
    sh "ls -alh"
    def resultsRoot = stageConfig.benchmarkResultsRoot
    if (resultsRoot == null) {
      resultsRoot = env.WORKSPACE + '/benchmark_results'
    }

    def configPath = stageConfig.benchmarkConfigPath
    if (configPath == null) {
      configPath = BENCHMARK_SUITE_DEFAULT_PATH
    }
    def benchmarkConfig = readConfig(configPath)

    benchmarkConfig.each{ id, spec ->
      def revisionResultsRoot = resultsRoot + "/${spec['commit']}_${id}"
      echo "###### Starting benchmarks for ${spec['commit']} ######"
      echo """Details:
      Revision:           ${spec['commit']}
      Benchmark script:   ${spec['benchmarkFile']}
      Environment file:   ${spec['envFile']}
      Results folder:     ${revisionResultsRoot}
      # od runs:          ${spec['numOfRuns']}
      Bucket for results: ${spec['bucket']}
      """

      dir (H2O_3_BENCHMARK_HOME) {
        sh "git checkout -f ${spec['commit']}"
      }
      def buildEnv = customEnv() + ["PYTHON_VERSION=${stageConfig.pythonVersion}", "R_VERSION=${stageConfig.rVersion}"]
      insideDocker(buildEnv, buildConfig, stageConfig.timeoutValue, 'MINUTES') {
        buildTarget {
          target = 'build-h2o-3'
          hasJUnit = false
          archiveFiles = false
          h2o3dir = H2O_3_BENCHMARK_HOME
        }
      }

      def range = (1..Integer.parseInt(spec['numOfRuns'])).toArray()
      for (runNum in range) {
        try {
          echo "###### Starting benchmark #${runNum} for ${spec['commit']} ######"
      //     def timestamp = new Date().getTime()
      //     if (!ensureFileExists(spec['benchmarkFile']) || !ensureFileExists(spec['envFile'])) {
      //         continue
      //     }
      //
      //     def benchScriptAbsolutePath = env.WORKSPACE + '/' + spec['benchmarkFile']
      //     def envFileAbsolutePath = env.WORKSPACE + '/' + spec['envFile']
      //
      //     // insideDockerEnv(....) {
      //       dir(revisionResultsRoot) {
      //         dir ("run${runNum}_${timestamp}") {
      //           def envList = []
      //           envList.addAll(readFile(envFileAbsolutePath).split('\n'))
      //           withEnv(envList) {
      //             sh "chmod +x ${benchScriptAbsolutePath}"
      //             sh "${benchScriptAbsolutePath}"
      //           }
      //         }
      //       }
      //     // }
        } finally {
          echo "###### Benchmark #${runNum} for ${spec['commit']} finished ######"
        }
      }
    }
  }
}

def readConfig(final String configPath) {
    if (fileExists(configPath)) {
        def benchmarkConfig = [:]
        readFile(configPath).split('\n').each{ line, count ->
            def values = line.split(",\\s*")
            if (values[0].toLowerCase() != 'id') {
                benchmarkConfig[values[0]] = [
                    commit: values[1],
                    numOfRuns: values[2],
                    benchmarkFile: values[3],
                    envFile: values[4],
                    bucket: values[5]
                ]
            }
        }
        return benchmarkConfig
    }
    error 'CSV file not found'
}

boolean ensureFileExists(final String path) {
    if (!fileExists(path)) {
        echo "[ERROR] Cannot find benchmark script ${path}"
        return false
    }
    return true
}

return this
