# Base de données

Ce dépôt contient l'infrastructure de base de données du projet MedicApp : schémas SQL, migrations, pipelines CI/CD et données de test.

---

## Table des matières

1. [Architecture des bases de données](#1-architecture-des-bases-de-données)
2. [Structure du projet](#2-structure-du-projet)
3. [Schémas SQL (sql/prod/)](#3-schémas-sql-sqlprod)
4. [Rôle des tables](#4-rôle-des-tables)
5. [Système de migrations](#5-système-de-migrations)
6. [Pipelines Jenkins](#6-pipelines-jenkins)
7. [Scripts d'automatisation](#7-scripts-dautomatisation)
8. [Base de données de test](#8-base-de-données-de-test--version-v1)

---

## 1. Architecture des bases de données

Le projet utilise une architecture à **quatre bases de données** :

| Base de données | Rôle |
|-----------------|------|
| `med_db_active` | Base de données principale de production contenant les données actives |
| `med_db_temp` | Base temporaire avec un schéma identique, utilisée pour les opérations de traitement |
| `med_db_archive` | Base d'archivage pour les données historiques |
| `med_db_views` | Base contenant des vues en lecture seule pointant vers `med_db_active` |

---

## 2. Structure du projet

```
basededonnee/
├── README.md                           # Ce fichier
├── docker-compose.yml                  # Configuration Docker pour environnement local/test
├── jenkins/                            # Pipelines CI/CD
│   ├── Jenkinsfile.deploy              # Pipeline de déploiement du serveur
│   └── Jenkinsfile.migrations          # Pipeline d'exécution des migrations
├── scripts/                            # Scripts d'automatisation
│   ├── deploy_and_execute_migrations.sh
│   └── apply_migrations_docker.sh
├── sql/
│   ├── prod/                           # Schémas de production
│   │   ├── 01_create_med_db_active.sql
│   │   ├── 01_create_med_db_temp.sql
│   │   ├── 01_create_med_db_archive.sql
│   │   ├── 01_create_med_db_views.sql
│   │   └── 02_create_db_users.sh       # Comptes applicatifs (API lecture seule + import limité)
│   ├── migrations/                     # Migrations SQL versionnées
│   │   └── V###__scope_description.sql
│   └── test/
│       └── 01_create_test_med_db.sql
```

Les fichiers CSV de test (V1) décrits en section 8 se trouvent dans `server/src/test/resources/data_test/`.

---

## 3. Schémas SQL (sql/prod/)

Les fichiers du dossier `sql/prod/` définissent les schémas initiaux des bases de données.

### 01_create_med_db_active.sql

Script de création de la base principale `med_db_active`. Contient toutes les tables métier, les clés étrangères, les index et la table de suivi des migrations (`schema_migrations`).

### 01_create_med_db_temp.sql

Script de création de `med_db_temp`. **Schéma identique** à `med_db_active`, utilisé comme espace de travail temporaire pour les traitements batch.

### 01_create_med_db_archive.sql

Script de création de `med_db_archive`. **Schéma identique** à `med_db_active`, destiné à stocker les données historiques.

### 01_create_med_db_views.sql

Script de création de `med_db_views`. Contient des **vues SQL** qui pointent vers les tables de `med_db_active`, permettant un accès en lecture seule avec une couche d'abstraction.

---

## 4. Rôle des tables

### Tables principales

| Table | Description |
|-------|-------------|
| `med_specialite` | **Table pivot** – Spécialités pharmaceutiques au sens réglementaire (CIS). Contient les informations d'AMM, statut, prescriptibilité, code ATC. |
| `med_presentation` | Présentations pharmaceutiques (CIP13) dispensées en pharmacie. Rattachées à une spécialité via le CIS. |
| `med_voie` | Référentiel des voies d'administration (orale, injectable, cutanée, etc.). |
| `med_specialite_voie` | Table de liaison N:N entre spécialités et voies d'administration. |

### Tables de composition

| Table | Description |
|-------|-------------|
| `med_composition_qualitative` | Substances composant chaque spécialité, sans indication de dosage. |
| `med_composition_quantitative` | Dosages associés aux substances (quantité, référence, fraction thérapeutique). |
| `med_substance` | Référentiel interne des substances actives. |
| `dictionnaire_sms` | Alignement avec le référentiel EMA/SMS (codes internationaux, formules moléculaires). |

### Tables d'éléments

| Table | Description |
|-------|-------------|
| `med_element_specialite` | Éléments fabriqués d'une spécialité (formes, supports de dosage). |
| `med_element_presentation` | Liaison entre éléments et présentations (CIP). |

### Tables d'événements

| Table | Description |
|-------|-------------|
| `med_evenement_cis` | Historique réglementaire des spécialités (AMM, retraits, modifications). |
| `med_evenement_cip` | Historique réglementaire des présentations. |

### Tables de référence

| Table | Description |
|-------|-------------|
| `med_organisation` | Titulaires d'AMM et laboratoires pharmaceutiques. |
| `med_groupe_generique` | Groupes de médicaments génériques. |
| `med_groupe_generique_membre` | Appartenance des spécialités aux groupes génériques. |

### Tables techniques

| Table | Description |
|-------|-------------|
| `schema_migrations` | Suivi des migrations appliquées (version, checksum, date). |
| `referentiel_import_state` | Historique des imports de données (source, version, hash, statut). |

---

## 5. Système de migrations

### Convention de nommage

Les migrations suivent un format inspiré de Flyway :

```
V[VERSION]__[SCOPE]_[DESCRIPTION].sql
```

**Exemples :**
- `V001__tables_test_column.sql` → Migration sur les tables
- `V002__views_test_column.sql` → Migration sur les vues

### Scopes disponibles

| Scope | Bases cibles |
|-------|--------------|
| `tables` | `med_db_active`, `med_db_temp` |
| `data` | `med_db_active`, `med_db_temp` |
| `index` | `med_db_active`, `med_db_temp` |
| `views` | `med_db_views` |

### Fonctionnement

1. Le script parse le nom du fichier pour extraire la version et le scope
2. Selon le scope, la migration est routée vers les bases appropriées
3. Avant exécution, le script vérifie si la migration a déjà été appliquée via la table `schema_migrations`
4. Après exécution, un enregistrement est créé avec le checksum SHA256 du fichier

### Créer une nouvelle migration

1. Créer un fichier dans `sql/migrations/` avec le prochain numéro de version
2. Respecter le format : `V###__[scope]_description.sql`
3. Commiter et pousser sur la branche appropriée
4. La pipeline Jenkins appliquera automatiquement la migration

---

## 6. Pipelines Jenkins

### Jenkinsfile.deploy

**Rôle :** Déploiement du serveur applicatif sur le VPS de production.

**Déclencheur :** Push sur la branche `main`

**Étapes :**
1. **Fetch Latest Code** – Récupération du code via SSH sur le VPS
2. **Build Docker Image** – Construction de l'image Docker
3. **Restart Service** – Redémarrage du service via docker compose
4. **Verify Deployment** – Vérification du statut des conteneurs

**Configuration :**
- VPS : `<VPS_HOST>`
- Utilisateur : `jenkins-deploy`
- Credentials : `vps-production-ssh`

### Jenkinsfile.migrations

**Rôle :** Application des migrations SQL sur les bases de production.

**Déclencheur :** Push sur la branche `main` du dépôt infrastructure

**Étapes :**
1. **Checkout Infrastructure** – Clonage du dépôt contenant les migrations
2. **List Migrations** – Vérification et listage des fichiers `V*.sql`
3. **Deploy to VPS** – Copie des migrations vers un répertoire temporaire sur le VPS
4. **Apply Migrations** – Exécution des migrations dans le conteneur MariaDB
5. **Cleanup** – Suppression des fichiers temporaires

**Configuration :**
- Conteneur MariaDB : `serveur-mariadb-1`
- Timeout : 10 minutes
- Credentials : `vps-production-ssh`, `git-infrastructure-ssh`

---

## 7. Scripts d'automatisation

### deploy_and_execute_migrations.sh

**Rôle :** Orchestration de l'exécution des migrations sur le VPS.

**Usage :**
```bash
./scripts/deploy_and_execute_migrations.sh <vps_user> <vps_host> <temp_dir> <mariadb_container>
```

**Exemple :**
```bash
./scripts/deploy_and_execute_migrations.sh jenkins-deploy <VPS_HOST> /tmp/migrations-run serveur-mariadb-1
```

**Actions :**
1. Validation des paramètres
2. Copie des migrations dans le conteneur MariaDB via `docker cp`
3. Exécution de `apply_migrations_docker.sh` dans le conteneur
4. Nettoyage des fichiers

### apply_migrations_docker.sh

**Rôle :** Script exécuté **à l'intérieur** du conteneur MariaDB pour appliquer les migrations.

**Fonctionnalités :**
- Parsing du scope depuis le nom de fichier
- Routage vers les bonnes bases de données
- Vérification d'idempotence (ne réapplique pas une migration déjà exécutée)
- Calcul du checksum SHA256
- Enregistrement dans `schema_migrations`

**Variables d'environnement utilisées :**
- `MARIADB_USER` – Utilisateur de connexion
- `MARIADB_PASSWORD` – Mot de passe

---

## 8. Base de données de test – Version V1

### 8.1. Objectif du document

Ce document décrit la **version V1 de la base de données de test** du projet MedicApp. Il a pour objectif de :

- expliciter les **scénarios métier couverts** par chaque fichier CSV ;
- documenter les **choix intentionnels** faits dans cette V1 ;
- servir de **référence commune** pour le développement backend, les tests et les futures évolutions ;
- permettre à un tiers (enseignant, jury, CPAM, développeur externe) de **comprendre rapidement la portée et les limites** de cette base de test.

> ⚠️ Cette base de test n’a **pas vocation à représenter fidèlement la volumétrie ou l’exhaustivité** du référentiel ANSM / RUIM. Elle est volontairement **réduite, scénarisée et explicable**.

---

### 8.2. Périmètre de la V1

La V1 de la base de test couvre :

- des **scénarios métier explicites** sur les tables centrales : `CIS.csv` et `CIP.csv` ;
- des **données cohérentes minimales** sur les tables dépendantes (compositions, éléments, événements, organisations, substances) ;
- un objectif prioritaire : **tester la logique métier, les jointures et les filtres**, et non la performance ou la complétude réglementaire.

Les tables suivantes sont incluses dans cette V1 :

- CIS.csv
- CIP.csv
- Compositions_qualitatives.csv
- Compositions_quantitatives.csv
- Elements.csv
- Elements_CIP.csv
- Evenements_CIS.csv
- Evenements_CIP.csv
- Organisations.csv
- Substances.csv
- SMS_Dictionnaire.csv
- Voies.csv
- Correspondance-UCD-CIP-CIS.csv

---

### 8.3. Fichier `CIS.csv` – Spécialités pharmaceutiques

#### Rôle

Le fichier `CIS.csv` est la **table pivot** du modèle. Il représente les spécialités pharmaceutiques au sens réglementaire (AMM, statut, prescriptibilité, voies).

#### Scénarios couverts (une ligne = un scénario)

Les lignes du fichier sont **intentionnellement ordonnées** et documentées comme suit :

1. **Spécialité active**
    - CIS : `60035924`

2. **Spécialité inactive (AMM retirée)**
    - CIS : `60262879`

3. **Spécialité avec une seule voie d’administration**
    - CIS : `66507634`

4. **Spécialité avec plusieurs voies d’administration**
    - CIS : `67217445`

5. **Spécialité prescriptible en dénomination commune (DC)**
    - CIS : `60033389`

6. **Spécialité non prescriptible en DC**
    - CIS : `60208447`

7. **Spécialité inactive mais encore liée à des présentations (CIP)**
    - CIS : `60262879`
    - Cas volontairement redondant pour tester la robustesse des jointures

---

### 8.4. Fichier `CIP.csv` – Présentations pharmaceutiques

#### Rôle

Le fichier `CIP.csv` décrit les **présentations réellement dispensées** en pharmacie et leur rattachement aux spécialités (CIS).

#### Organisation générale

- Les **premières lignes** correspondent exclusivement aux CIS présents dans `CIS.csv`.
- Aucun CIP orphelin n’est présent dans la V1.

#### Scénarios couverts

- **1 CIS → 1 CIP**
    - Lignes 2 à 4
    - CIP : `3400949215539`, `3400949020539`, `3400956434299`

- **1 CIS → plusieurs CIP**
    - Lignes 5 et 6
    - CIP : `3400956424306`, `3400956810680`

- **CIP sans code CIP7**
    - Ligne 7
    - CIP : `3400930116579`

- **CIP avec champ `type_dispositif` renseigné**
    - Ligne 2
    - CIP : `3400949020539`

---

### 8.5. Fichier `Compositions_qualitatives.csv`

#### Rôle

Décrire **les substances composant chaque spécialité**, indépendamment du dosage.

#### Scénarios couverts

- Lignes 2–3 : spécialité active (`60035924`)
- Lignes 4–6 : spécialité inactive (`60262879`)
- Lignes 7–8 : spécialité à voie unique (`66507634`)
- Lignes 9–10 : spécialité à voies multiples (`67217445`)
- Lignes 11–12 : spécialité prescriptible en DC (`60033389`)
- Lignes 13–39 : spécialité non prescriptible en DC (`60208447`)
- Lignes 40–42 : spécialité inactive mais encore liée à des CIP (`60262879`)

---

### 8.6. Fichier `Compositions_quantitatives.csv`

#### Rôle

Décrire les **dosages** associés aux substances.

#### Scénarios couverts

- Ligne 2 : spécialité active (`60035924`)
- Ligne 3 : spécialité inactive (`60262879`)
- Ligne 4 : spécialité à voie unique (`66507634`)
- Ligne 5 : spécialité à voies multiples (`67217445`)
- Ligne 6 : spécialité inactive encore liée à des CIP (`60262879`)

---

### 8.7. Fichier `Elements.csv`

#### Rôle

Représenter les **éléments fabriqués** (formes, supports de dosage) d'une spécialité.

#### Scénarios couverts

- Ligne 2 : spécialité active (`60035924`)
- Ligne 3 : spécialité inactive (`60262879`)
- Ligne 4 : spécialité à voie unique (`66507634`)
- Ligne 5 : spécialité à voies multiples (`67217445`)
- Ligne 6 : spécialité prescriptible en DC (`60033389`)
- Ligne 7 : spécialité non prescriptible en DC (`60208447`)
- Ligne 8 : spécialité inactive mais encore liée à des CIP (`60262879`)

---

### 8.8. Fichier `Elements_CIP.csv`

#### Rôle

Faire le lien entre **éléments fabriqués** et **présentations (CIP)**.

#### État en V1

- Données cohérentes avec `Elements.csv` et `CIP.csv`.
- Aucun scénario métier spécifique encore isolé.

#### Scénarios prévus (V2)

- 1 CIP → plusieurs éléments
- élément partagé entre plusieurs CIP

---

### 8.9. Fichiers `Evenements_CIS.csv` et `Evenements_CIP.csv`

#### Rôle

Historiser la **vie réglementaire** des spécialités et des présentations.

#### Evenements_CIS – Scénarios

- Ligne 2 : spécialité active (`60035924`)
- Ligne 3 : spécialité inactive (`60262879`)
- Ligne 4 : spécialité à voie unique (`66507634`)
- Ligne 5 : spécialité à voies multiples (`67217445`)
- Ligne 6 : spécialité prescriptible en DC (`60033389`)
- Ligne 7 : spécialité non prescriptible en DC (`60208447`)
- Lignes 8–9 : spécialité inactive mais encore liée à des CIP (`60262879`)

#### Evenements_CIP – Scénarios

- Ligne 2 : `3400949215539`
- Lignes 3–4 : `3400949020539`
- Lignes 5–7 : `3400956434299`
- Ligne 8 : `3400956424306`
- Ligne 9 : `3400956810680`
- Ligne 10 : `3400930116579`
- Lignes 11–12 : `3400957043032`
- Lignes 13–14 : `3400949020539`

---

### 8.10. Fichier `Organisations.csv`

#### Rôle

Décrire les **titulaires d'AMM** référencés dans `CIS.csv`.

#### État en V1

- Contient uniquement les organisations nécessaires aux CIS présents.
- Aucun scénario métier spécifique isolé à ce stade.

#### Scénarios prévus (V2)

- co-titulaires ;
- changement de titulaire ;
- organisation étrangère.

---

### 8.11. Fichiers `Substances.csv` et `SMS_Dictionnaire.csv`

#### Rôle

- `Substances.csv` : référentiel interne des substances utilisées.
- `SMS_Dictionnaire.csv` : alignement avec le référentiel EMA / SMS.

#### État en V1

- Toutes les substances référencées dans les compositions qualitatives sont présentes.
- Chaque substance possède une entrée correspondante dans le SMS.
