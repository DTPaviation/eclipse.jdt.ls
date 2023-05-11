pipeline {
  agent any
  stages {
    stage('Set Parameters') {
      steps {
        script {
          env.flag = '0'
          properties([
            parameters([
            string(name: 'BRANCH_NAME', defaultValue: 'eclipse.jdt.ls/master', description: 'JOB_NAME/BRANCH_NAME'),
            booleanParam(name: 'BUILD_AND_DEPLOY_NEW_LSP_SERVER', defaultValue: true, description: 'BUILD AND DEPLOY NEW_LSP IMAGE'),
            booleanParam(name: 'CHECK_LATEST_PIPELINE', defaultValue: false, description: 'CHECK_LATEST_PIPELINE'),            
            string(name: 'PREFIX', defaultValue: 'LSP_SERVER', description: 'PREFIX'),
            string(name: 'KUBERNETES_IP', defaultValue: '192.168.102.134', description: 'The Kubernetes host IP,For Tripoli Office use: 192.168.1.20'),
            string(name: 'DOCKER_REG_IP_PORT', defaultValue: '192.168.102.123:5000', description: 'The Docker register host:port used by skaffold, For Tripoli Office use: 192.168.1.95:5000'),
            string(name: 'NameSpace', defaultValue: 'tnexus-mongodb', description: ' Kubernetes Namespace')
          ])
        ])
      }

    }
  }

  stage('Git hard reset') {
    steps {
      echo 'Git hard reset and pull origin from "monaco-lsp-server/jdt_ls_build"'
    }
  }

  stage('CHECKUPS: ') {
    parallel {
      stage('Log tools versions and varaiables') {
        when {
          expression {
            return params.CHECK_LATEST_PIPELINE
          }

        }
        steps {
          echo 'check Version'
          sh 'printenv'
          sh 'docker -v'
          sh 'echo $GENERATED_VERSION_NUMBER'
        }
      }

      stage('Git:Check config') {
        when {
          expression {
            return params.CHECK_LATEST_PIPELINE
          }

        }
        steps {
          echo 'Git hard reset and pull origin from "$GRAMMAR_CODE_BRANCH"'
          sh 'echo "Service user is $SERVICE_CREDS_USR"'
          sh 'echo "Service password is $SERVICE_CREDS_PSW"'
          sh 'git config -l'
        }
      }

      stage('Check Dockerfile exist') {
        when {
          expression {
            return params.CHECK_LATEST_PIPELINE
          }

        }
        steps {
          fileExists 'Dockerfile'
        }
      }

    }
  }
  stage('Maven Build') {
    when {
      expression {
        return params.BUILD_AND_DEPLOY_NEW_LSP_SERVER
      }

    }
    steps {
        echo 'CLean before build'
		sh '''
		git reset --hard
		git clean -dfx
		'''
		sh '''
		mvn clean
		'''
		echo 'Maven BUILD_AND_DEPLOY_NEW_LSP_SERVER'
        sh '''
        export M2_HOME=/var/lib/jenkins/tools/hudson.tasks.Maven_MavenInstallation/maven-3.8.3
        export MAVEN_HOME=/var/lib/jenkins/tools/hudson.tasks.Maven_MavenInstallation/maven-3.8.3
        export KUBECONFIG=/var/lib/jenkins/newjenkins-k8sv25-config-2022
        mvn -version
        mvn -f pom.xml -Dcbi.jarsigner.skip=false -DskipTests=true package -B -e -Pserver-distro,update-site
        '''
		sh 'mv org.eclipse.jdt.ls.product/target/repository/plugins/org.eclipse.equinox.launcher_*.jar org.eclipse.jdt.ls.product/target/repository/plugins/org.eclipse.equinox.launcher_Latest.jar'
    }
  }
    stage('Skaffold: Build and deploy image ') {
      when {
          expression {
            return params.BUILD_AND_DEPLOY_NEW_LSP_SERVER
          }
      }
      steps {
        echo 'Deploying image on Kubernetes using Skaffold'
        sh '''
        export M2_HOME=/var/lib/jenkins/tools/hudson.tasks.Maven_MavenInstallation/maven-3.8.3
        export MAVEN_HOME=/var/lib/jenkins/tools/hudson.tasks.Maven_MavenInstallation/maven-3.8.3
        export KUBECONFIG=/var/lib/jenkins/newjenkins-k8sv25-config-2022
        find . -type f -path "*/k8s/*" -name "*deployment.yml" -print0 | xargs -0 sed  -i "s/kubernetesMaster/$KUBERNETES_IP/g"
        skaffold run --namespace=$NameSpace --default-repo=$DOCKER_REG_IP_PORT --cleanup=false --event-log-file=\'skafflod-events.log\' --enable-rpc=true --no-prune=true --no-prune-children=true
        '''
        script {  env.flag ='1'  }
      }
    }  

}
  post {
    success {
      script {
      currentBuild.displayName = "${GENERATED_VERSION_NUMBER}"

        env.message = 'Monaco_LSP_SERVER Pipeline: Successfully deployed LSP_SERVER'
 
      if (params.BUILD_AND_DEPLOY_NEW_LSP_SERVER) {
        env.message = "${message}" + ' JDTLS'
      }
      
      if (env.flag == '1' )
      hangoutsNotify (message: " ${env.message}", token: 'HUWPuO6FRsaVaKx7z4B5Yi5lq', threadByJob: false)       
      }
    }
    failure {
      script {
      currentBuild.displayName = "${GENERATED_VERSION_NUMBER}"
      }
      hangoutsNotify (message: " Monaco_LSP_SERVER Pipeline: Job failed", token: 'HUWPuO6FRsaVaKx7z4B5Yi5lq', threadByJob: false)
        //chat 'Rtdo9XtKWAfnFGLXMu4VcQQo-'
    }
  }

tools {
  jdk 'temurin-17-0.3'
  maven 'maven-3.8.6'
}
environment {
  DOCKER_BUILDKIT = 1
  GENERATED_VERSION_NUMBER = VersionNumber( projectStartDate: '2023-01-31', versionNumberString: 'LSP_SERVER-${BUILD_DATE_FORMATTED, "yyyy-MM-dd"}-${BUILDS_TODAY}', versionPrefix: '', worstResultForIncrement: 'SUCCESS' )
}
options {
buildDiscarder(logRotator(numToKeepStr: '16', artifactNumToKeepStr: '16'))
durabilityHint('PERFORMANCE_OPTIMIZED')
timestamps()
}
}
