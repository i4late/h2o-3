def call(buildConfig, stageConfig) {

  def defaultStage = load('h2o-3/scripts/jenkins/groovy/defaultStage.groovy')
  def stageNameToDirName = load('h2o-3/scripts/jenkins/groovy/stageNameToDirName.groovy')

  if (stageConfig.bucket == null) {
    stageConfig.bucket = 's3://test.0xdata.com/h2o-3-benchmarks'
  }
  if (stageConfig.datasetsPath == null) {
    stageConfig.datasetsPath = 'h2oR/accuracy_datasets_h2o.csv'
  }
  if (stageConfig.testCasesPath == null) {
    stageConfig.testCasesPath = 'h2oR/test_cases_dev.csv'
  }
  if (stageConfig.benchmarkResultsRoot == null) {
    stageConfig.benchmarkResultsRoot = "${env.WORKSPACE}/benchmark_results/${stageNameToDirName(stageConfig.stageName)}"
  }
  sh "mkdir -p ${stageConfig.benchmarkResultsRoot}"

  def mlBenchmarkRoot = "${env.WORKSPACE}/${stageNameToDirName(stageConfig.stageName)}/h2o-3/ml-benchmark"
  dir (mlBenchmarkRoot) {
    checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/mr/feature/size-benchmarking']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'c6bab81a-6bb5-4497-9ec9-285ef5db36ea', url: 'https://github.com/h2oai/ml-benchmark']]]
  }
  withEnv(["OUTPUT_PREFIX=${stageConfig.benchmarkResultsRoot}", "DATASETS_PATH=${mlBenchmarkRoot}/${stageConfig.datasetsPath}", "TEST_CASES_PATH=${mlBenchmarkRoot}/${stageConfig.testCasesPath}"]) {
    defaultStage(buildConfig, stageConfig)
  }
}

return this
