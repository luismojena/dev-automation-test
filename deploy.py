#!/usr/bin/env python3
import click
import subprocess
import os

# mysql_root_password = os.environ["MYSQL_ROOT_PASSWORD"]
mysql_root_password = "mHsJ33lF+1FZ"


def run_query(query):
    process = 'mysql -h db -u root -p{} -s -N -e "{}"'.format(mysql_root_password, query)
    process_handler = subprocess.Popen(process, stdout=subprocess.PIPE, shell=True)
    return process_handler.stdout.read()


@click.group()
def main():
    pass


@main.command()
@click.argument("migration_name")
def check_migration(migration_name):
    click.echo("Checking migrations ...")
    print(
        run_query(
            "select m.id, t.id, t.name from rmtest.migrations as m join rmtest.tenants as t on () where name='{}'".format(migration_name)
        )
    )


@main.command()
def count_migrations():
    pass


if __name__ == "__main__":
    main()
