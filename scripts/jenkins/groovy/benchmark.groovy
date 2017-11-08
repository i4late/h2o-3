def call(buildConfig, stageConfig) {

  def H2O_3_HOME = 'h2o-3'

  if (stageConfig.bucket == null) {
    stageConfig.bucket = 'test.0xdata.com/h2o-3-benchmarks'
  }

  def insideDocker = load('h2o-3/scripts/jenkins/groovy/insideDocker.groovy')
  def buildTarget = load('h2o-3/scripts/jenkins/groovy/buildTarget.groovy')
  def customEnv = load('h2o-3/scripts/jenkins/groovy/customEnv.groovy')

  stage (stageConfig.stageName) {

    writeFile file: 'benchmark.sh', text: """#! /bin/bash
echo Benchmark test
printenv
"""

    dir ('ml-benchmark') {
      checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'c6bab81a-6bb5-4497-9ec9-285ef5db36ea', url: 'https://github.com/h2oai/ml-benchmark']]]
    }

    def resultsRoot = stageConfig.benchmarkResultsRoot
    if (resultsRoot == null) {
      resultsRoot = env.WORKSPACE + '/benchmark_results'
    }

    def revisionResultsRoot = resultsRoot + "/${stageConfig.commit}_${new Date().getTime()}"
    echo "###### Starting benchmarks for ${stageConfig.commit} ######"
    echo """Details:
    Revision:           ${stageConfig.commit}
    Benchmark script:   ${stageConfig.benchmarkFile}
    Results folder:     ${revisionResultsRoot}
    Bucket for results: ${stageConfig.bucket}
    """

    echo "###### Starting benchmark #${runNum} for ${stageConfig.commit} ######"
    def timestamp = new Date().getTime()
    if (!ensureFileExists(spec['benchmarkFile']) || !ensureFileExists(spec['envFile'])) {
        continue
    }

    def benchScriptAbsolutePath = env.WORKSPACE + '/' + spec['benchmarkFile']

    // insideDockerEnv(....) {
    //   dir(revisionResultsRoot) {
    //     dir ("run${runNum}_${timestamp}") {
    //       def envList = []
    //       envList.addAll(readFile(envFileAbsolutePath).split('\n'))
    //       withEnv(envList) {
    //         sh "chmod +x ${benchScriptAbsolutePath}"
    //         sh "${benchScriptAbsolutePath}"
    //       }
    //     }
    //   }
    // }
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
