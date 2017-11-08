def call(buildConfig, stageConfig) {

  def BENCHMARK_SUITE_DEFAULT_PATH = 'benchmarking_suite.csv'

  stage (stageConfig.stageName) {
    writeFile file: BENCHMARK_SUITE_DEFAULT_PATH, text: """id,commit,num_of_runs,benchmark_file,env_file,bucket
1,master,1,benchmark.sh,env-file,test.0xdata.com/h2o-3-benchmarks
2,rel-weierstrass,3,benchmark.sh,env-file,test.0xdata.com/h2o-3-benchmarks
"""
    writeFile file: 'env-file', text: """DUMMY_VAR='BENCH VAR'
TEST_VAR='TEST VAR'
"""
    writeFile file: 'benchmark.sh', text: """#! /bin/bash
echo Benchmark test
printenv
echo \${DUMMY_VAR}
echo \${TEST_VAR}
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
    echo "${benchmarkConfig}"
  }
}

def readConfig(final String configPath) {
    if (fileExists(configPath)) {
        def config = [:]
        readFile(configPath).split('\n').each{ line, count ->
            def values = line.split(",\\s*")
            if (values[0].toLowerCase() != 'id') {
                config[values[0]] = [
                    commit: values[1],
                    numOfRuns: values[2],
                    benchmarkFile: values[3],
                    envFile: values[4],
                    bucket: values[5]
                ]
            }
        }
        return config
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

// def wip() {
//     stage ('Preparation') {
//
//         writeFile file: BENCHMARK_SUITE_DEFAULT_PATH, text: """id,commit,num_of_runs,benchmark_file,env_file,bucket
// 1,master,1,benchmark.sh,env-file,test.0xdata.com/h2o-3-benchmarks
// 2,rel-weierstrass,3,benchmark.sh,env-file,test.0xdata.com/h2o-3-benchmarks
// """
//         writeFile file: 'env-file', text: """DUMMY_VAR='BENCH VAR'
// TEST_VAR='TEST VAR'
// """
//
//         writeFile file: 'benchmark.sh', text: """#! /bin/bash
// echo Benchmark test
// printenv
// echo \${DUMMY_VAR}
// echo \${TEST_VAR}
// """
//     }
//
//     stage ('Benchmark') {
//
//         config.each{ id, spec ->
//             def revisionResultsRoot = resultsRoot + "/${spec['commit']}_${id}"
//             echo "###### Starting benchmarks for ${spec['commit']} ######"
//             echo """Details:
//             Revision:           ${spec['commit']}
//             Benchmark script:   ${spec['benchmarkFile']}
//             Environment file:   ${spec['envFile']}
//             Results folder:     ${revisionResultsRoot}
//             # od runs:          ${spec['numOfRuns']}
//             Bucket for results: ${spec['bucket']}
//             """
//
//             dir ('h2o-3-benchmark') {
//                 retry(3) {
//                     timeout(time: 1, unit: 'MINUTES') {
//                         checkout([$class: 'GitSCM', branches: [[name: spec['commit'] ]],
//                             userRemoteConfigs: [[url: 'https://github.com/h2oai/h2o-3']]])
//                     }
//                 }
//             }
//
//             // insideDocker(....) {
//             //      buildH2O3()
//             // }
//
//             def range = (1..Integer.parseInt(spec['numOfRuns'])).toArray()
//             for (runNum in range) {
//                 try {
//                     echo "###### Starting benchmark #${runNum} for ${spec['commit']} ######"
//                     def timestamp = new Date().getTime()
//                     if (!ensureFileExists(spec['benchmarkFile']) || !ensureFileExists(spec['envFile'])) {
//                         continue
//                     }
//
//                     def benchScriptAbsolutePath = env.WORKSPACE + '/' + spec['benchmarkFile']
//                     def envFileAbsolutePath = env.WORKSPACE + '/' + spec['envFile']
//
//                     // insideDockerEnv(....) {
//                         dir(revisionResultsRoot) {
//                             dir ("run${runNum}_${timestamp}") {
//                                 def envList = []
//                                 envList.addAll(readFile(envFileAbsolutePath).split('\n'))
//                                 withEnv(envList) {
//                                     sh "chmod +x ${benchScriptAbsolutePath}"
//                                     sh "${benchScriptAbsolutePath}"
//                                 }
//                             }
//                         }
//                     // }
//                 } finally {
//                     echo "###### Benchmark #${runNum} for ${spec['commit']} finished ######"
//                 }
//             }
//         }
//     }
// }

return this
