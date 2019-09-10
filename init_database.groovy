pipeline {
    agent any
    stages {
        stage('Init DB') {
            steps {
                sh '''
                   mysql -h db -u root -p=$MYSQL_ROOT_PASSWORD < /data/database.sql
                '''
            }
        }
    }
}