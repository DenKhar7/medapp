#!/bin/bash
#
# Script : deploy_and_execute_migrations.sh
# Description : Déploie et exécute les migrations SQL sur le serveur de production
# Usage : ./deploy_and_execute_migrations.sh <user> <host> <temp_dir> <container_name>
# Exemple : ./deploy_and_execute_migrations.sh jenkins-deploy <VPS_HOST> /tmp/migrations-run serveur-mariadb-1
#

set -e

# Validation des paramètres
if [ $# -ne 4 ]; then
    echo "Erreur: Nombre incorrect de paramètres"
    echo "Usage: $0 <vps_user> <vps_host> <temp_dir> <mariadb_container>"
    echo ""
    echo "Exemple:"
    echo "$0 jenkins-deploy <VPS_HOST> /tmp/migrations-run serveur-mariadb-1"
    exit 1
fi

# Paramètres
VPS_USER=$1
VPS_HOST=$2
TEMP_DIR=$3
MARIADB_DOCKER_NAME=$4

echo "Déploiement et exécution des migrations"
echo "VPS: ${VPS_USER}@${VPS_HOST}"
echo "Conteneur: ${MARIADB_DOCKER_NAME}"
echo "Dossier temporaire: ${TEMP_DIR}"
echo ""

# Exécution SSH avec interpolation des variables
ssh ${VPS_USER}@${VPS_HOST} << ENDSSH
set -e

echo "Copie des fichiers dans le conteneur MariaDB..."
docker cp ${TEMP_DIR}/migrations ${MARIADB_DOCKER_NAME}:/migrations
docker cp ${TEMP_DIR}/scripts/apply_migrations_docker.sh ${MARIADB_DOCKER_NAME}:/apply_migrations.sh

echo ""
echo "Exécution des migrations..."
docker exec ${MARIADB_DOCKER_NAME} bash /apply_migrations.sh

echo ""
echo "Nettoyage dans le conteneur..."
docker exec ${MARIADB_DOCKER_NAME} rm -rf /migrations /apply_migrations.sh
ENDSSH

echo ""
echo "Migrations déployées et exécutées avec succès"
