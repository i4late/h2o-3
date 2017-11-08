def call(buildConfig, stageConfig) {

  def H2O_3_HOME = 'h2o-3'

  if (stageConfig.bucket == null) {
    stageConfig.bucket = 's3://test.0xdata.com/h2o-3-benchmarks'
  }
  if (stageConfig.datasetsPath == null) {
    stageConfig.datasetsPath = 'h2oR/accuracy_datasets_h2o.csv'
  }
  if (stageConfig.testCasesPath == null) {
    stageConfig.testCasesPath = 'h2oR/testCasesPath_h2o.csv'
  }

  def insideDocker = load('h2o-3/scripts/jenkins/groovy/insideDocker.groovy')
  def buildTarget = load('h2o-3/scripts/jenkins/groovy/buildTarget.groovy')
  def customEnv = load('h2o-3/scripts/jenkins/groovy/customEnv.groovy')
  def defaultStage = load('h2o-3/scripts/jenkins/groovy/defaultStage.groovy')
  def stageNameToDirName = load('h2o-3/scripts/jenkins/groovy/stageNameToDirName.groovy')

  def mlBenchmarkRoot = "${env.WORKSPACE}/${stageNameToDirName(stageConfig.stageName)}/h2o-3/ml-benchmark"
  dir (mlBenchmarkRoot) {
    checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'c6bab81a-6bb5-4497-9ec9-285ef5db36ea', url: 'https://github.com/h2oai/ml-benchmark']]]
  }
  if (stageConfig.benchmarkResultsRoot == null) {
    stageConfig.benchmarkResultsRoot = "${env.WORKSPACE}/benchmark_results/${stageConfig.stageName}"
  }
  withEnv(["OUTPUT_PREFIX=${stageConfig.benchmarkResultsRoot}", "DATASETS_PATH=${mlBenchmarkRoot}/${stageConfig.datasetsPath}", , "TEST_CASES_PATH=${mlBenchmarkRoot}/${stageConfig.testCasesPath}"]) {
    defaultStage(buildConfig, stageConfig)
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
