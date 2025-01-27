pipeline {
    agent any
    stages {
        stage('Setup migrations') {
            steps {
                script{
                    def tenants = params.TENANT_NAMES.split(",")
                    echo "Tenant list: ${tenants}"
                    def migration_name = params.MIGRATION_NAME.trim()
                    echo "Migration name: ${migration_name}"

                    if (tenants[0] == "ALL") {
                        tenants = get_all_tenants_names()
                    }

                    // build 5 workers that pop tenants from a queue
                    LinkedBlockingQueue queue = new LinkedBlockingQueue()
                    def workers = [:]

                    for (int i = 1; i <= NUMBER_OF_WORKERS; i++) {
                        def index = i
                        def stepName = "Worker ${index}"
                        def currentQueue = queue

                        workers[stepName] = {
                            tenant = currentQueue.poll()
                            while(tenant!=null){
                                process_selected_tenant(tenant)
                                tenant = currentQueue.poll()
                            }
                        }
                    }

                    for (t in tenants){
                        queue.put(t)
                    }
                    echo "${queue}"

                    stage('Running migrations'){
                       parallel(workers)
                        echo "${queue}"
                    }
                }
            }
        }
    }
}

def process_selected_tenants(tenants){
    tenants.each { tenant ->
        def tenant_name = tenant.trim()
        echo "Processing tenant: ${tenant_name}"
        def tenant_id = get_tenant_id(tenant_name)
        echo "TENANT ID: ${tenant_id}"
        run_migration_for_tenant(tenant_id, migration_name)
}

def get_tenant_id(tenant_name){
    def query = "use rmtest; select id from tenants where name=\"${tenant_name}\";"
    tenant_id = run_mysql_query(query)
    return tenant_id.toInteger()
}

def run_migration_for_tenant(tenant_id, migration_name){
    def query = "use rmtest; INSERT INTO migrations(tenant_id, name) VALUES (${tenant_id}, \"${migration_name}\")"
    run_mysql_query(query, false)
}

def get_all_tenants_names(){
    def query = "use rmtest; SELECT name from tenants;"
    tenant_names = run_mysql_query(query).split("\n")
    return tenant_names
}

def run_mysql_query(query, showOutput=true){
    def result = sh(
        returnStdout: showOutput,
        script: "mysql -h db -u root -p$MYSQL_ROOT_PASSWORD -s -N -e \'${query}\' "
    )
    if (showOutput){
        return result.trim()
    }
    return result
}