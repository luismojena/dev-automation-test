pipeline {
    agent any
    stages {
        stage('Run migrations') {
            steps {
                script{
                    def tenants = params.TENANT_NAMES.split(",")
                    echo "Tenant list: ${tenants}"
                    def migration_name = params.MIGRATION_NAME.trim()
                    echo "Migraiton name: ${migration_name}"
                    tenants.each { tenant ->
                        def tenant_name = tenant.trim()
                        echo "Processing tenant: ${tenant_name}"
                        def tenant_id = get_tenant_id(tenant_name)
                        echo "TENANT ID: ${tenant_id}"
                        run_migration_for_tenant(tenant_id, migration_name)
                    }
                }
            }
        }
    }
}

def get_tenant_id(tenant_name){
    def query = "use rmtest; select id from tenants where name=\"${tenant_name}\";"
    def var = sh(
        returnStdout: true,
        script: "mysql -h db -u root -p$MYSQL_ROOT_PASSWORD -s -N -e \'${query}\' "
    ).trim()
    return var.toInteger()
}

def run_migration_for_tenant(tenant_id, migration_name){
    def query = "use rmtest; INSERT INTO migrations(tenant_id, name) VALUES (${tenant_id}, \"${migration_name}\")"
    sh(
        script: "mysql -h db -u root -p$MYSQL_ROOT_PASSWORD -s -N -e \'${query}\' "
    )
}