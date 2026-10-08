# Architecture du serveur

Ce serveur backend est développé en **Kotlin avec Ktor** et repose sur une **architecture hexagonale (Ports & Adapters)**. Cette architecture vise à isoler la logique métier des aspects techniques (API, base de données), afin d’améliorer la lisibilité, la maintenabilité et l’évolutivité du projet.

---

## Principe général

L’architecture est organisée autour de trois couches principales :

* **Domain** : cœur métier de l’application
* **Infrastructure** : implémentations techniques (base de données)
* **Interfaces (API)** : exposition des fonctionnalités via HTTP

Les dépendances sont toujours orientées **vers le domaine**, qui ne dépend d’aucune autre couche.

---

## Domain – Cœur métier

📁 `domain`

Cette couche contient la **logique métier** de l’application, écrite en Kotlin pur.

Elle regroupe :

* Les **modèles métier** (médicament, spécialité, substance, etc.)
* Les **services métier** (ex. `MedicamentService`)
* Les **interfaces de repositories** (ports)

Le domaine ne connaît ni la base de données, ni Ktor, ni les routes HTTP.

---

## Infrastructure – Accès aux données

📁 `infrastructure`

Cette couche fournit les **implémentations concrètes** des interfaces définies dans le domaine.

Elle contient :

* Les implémentations des repositories (ex. MariaDB)
* Les définitions des objets de mapping SQL
* Les requêtes SQL

Elle dépend du domaine, mais celui-ci reste totalement indépendant.

---

## Interfaces (API) – Exposition HTTP

📁 `interfaces` (ou `api`)

Cette couche gère l’**interaction avec les clients** via des routes HTTP.

Elle est responsable de :

* La définition des routes
* La réception et la validation des requêtes
* L’appel aux services du domaine
* La construction des réponses HTTP

Aucune logique métier n’est présente dans cette couche.

---

# Documentation – Modèles métier & API Médicament

Cette section décrit les **modèles métier exposés par l’API** et destinés à être consommés par l’application mobile.

> Ces modèles ne correspondent pas directement aux tables de base de données.
> Ils représentent des **vues métier stables**, pensées pour les usages côté mobile.

---

### Objectifs de cette documentation

- Décrire les objets retournés par l’API
- Clarifier le sens fonctionnel de chaque modèle
- Préciser les conventions (nullabilité, identifiants, formats)
- Faciliter l’intégration côté Android

---

## Modèles métier

### MedicamentSpeResume

**Usage principal :**  
Obtenir des informations sur une spécialité sans trop de détail.

**Description :**  
Résume les informations sur une spécialité.

**Champs :**

| Champ | Type | Description |
|------|------|-------------|
| cis | Int | Correspond au code identifiant de la spécialité pharmaceutique (code CIS ANSM). |
| nom | String | Nom de la spécialité pharmaceutique (libellé ANSM). |
| nomOrganisation | String? | Nom de l’organisation titulaire de l’autorisation de mise sur le marché (AMM). Peut être absent. |
| codeAtc | String? | Code ATC associé à la spécialité. Peut être absent si non renseigné dans les données source. |
| libelleAtc | String? | Libellé correspondant au code ATC. Peut être absent. |
| voies | List&lt;String&gt; | Liste des voies d’administration associées à la spécialité (libellés). La liste peut être vide mais n’est jamais nulle. |

**Exemple JSON :**
```json
{
    "cis": 68268147,
    "nom": "THIOCOLCHICOSIDE VIATRIS 4 mg, comprimé",
    "nomOrganisation": "VIATRIS SANTE",
    "codeAtc": "M03BX05",
    "libelleAtc": "Thiocolchicoside",
    "voies": [
        "orale"
    ]
}
```
### MedicamentPresDetail

**Usage principal :**  
Obtenir des informations sur une présentation avec les informations sur la spécialité à laquelle elle est associé.

**Description :**  
Détaille les informations sur la présentation et sa spécialité associé.

**Champs :**

| Champ | Type | Description |
|------|------|-------------|
| cip13 | String | Identifiant de la présentation pharmaceutique (code CIP à 13 chiffres). |
| label | String | Libellé ANSM de la présentation pharmaceutique. |
| nomSpecialite | String | Nom de la spécialité pharmaceutique associée à la présentation. |
| cis | Int | Code identifiant de la spécialité pharmaceutique (code CIS ANSM). |
| nomOrganisation | String? | Nom de l’organisation titulaire de l’autorisation de mise sur le marché (AMM). Peut être absent. |
| codeAtc | String? | Code ATC associé à la spécialité. Peut être absent si non renseigné dans les données source. |
| libelleAtc | String? | Libellé correspondant au code ATC. Peut être absent. |
| voie | String | Voie d’administration de la présentation pharmaceutique. |
| dosesParBoite | Int? | Nombre de doses contenues dans la boîte. Peut être absent selon la présentation. |
| quantiteConditionnement | Double? | Quantité de produit contenue dans le conditionnement primaire. Peut être absent. |
| uniteConditionnement | String? | Unité associée à la quantité du conditionnement (ex. mg, ml, comprimé). Peut être absente. |
| typeDispositif | String? | Type de dispositif d’administration associé à la présentation (ex. seringue, flacon). Peut être absent. |
| forme | List&lt;String&gt;? | Liste des formes pharmaceutiques associées à la présentation. Peut être absente. |
| unitPresentation | String? | Unité de présentation associée à la présentation pharmaceutique. Peut être absente. |
| typeDose | String? | Type de dose associée à la présentation (ex. unidose, multidose). Peut être absent. |

**Exemple JSON :**
```json
{
    "cip13": "3400935606105",
    "label": "THIOCOLCHICOSIDE VIATRIS 4 mg, comprimé - plaquette(s) thermoformée(s) PVC-aluminium PVDC de 12 comprimé(s)",
    "nomSpecialite": "THIOCOLCHICOSIDE VIATRIS 4 mg, comprimé",
    "cis": 68268147,
    "nomOrganisation": "VIATRIS SANTE",
    "codeAtc": "M03BX05",
    "libelleAtc": "Thiocolchicoside",
    "voie": "orale",

    "dosesParBoite": 12,
    "quantiteConditionnement": 12,
    "uniteConditionnement": "comprimé",
    "typeDispositif": "plaquette thermoformée",
    "forme": ["comprimé"],
    "unitPresentation": "boîte",
    "typeDose": "unidose"
}
```

### MedicamentSubstanceResume

**Usage principal :**  
Obtenir des informations sur les substances associé à une spécialité

**Description :**  
Donne les informations utiles relatif à la substance.

**Champs :**

| Champ | Type | Description |
|------|------|-------------|
| codeSubstance | String | Identifiant de la substance (code issu du référentiel des substances). |
| libelle | String | Libellé de la substance. |
| relationSubstance | String | Type de relation entre la substance et le médicament (ex. substance active, fraction thérapeutique, etc.). |
| expressionQuantite | String? | Expression textuelle de la quantité associée à la substance (ex. dosage exprimé). Peut être absente. |
| substanceActive | String? | Indique la substance active associée à l’expression de dosage, sous forme de code de substance. Peut être absente. |
| fractionTherapeutique | String? | Indique la fraction thérapeutique associée à l’expression de dosage, sous forme de code de substance. Peut être absente. |

**Exemple JSON**
```json
{
    "codeSubstance": "36246",
    "libelle": "THIOCOLCHICOSIDE",
    "relationSubstance": "substance active",
    "expressionQuantite": "4 mg par comprimé",
    "substanceActive": "68776",
    "fractionTherapeutique": "76589"
}
```

### MedicamentSubstanceDetail

**Usage principal :**  
Obtenir des informations détaillé sur une substance associé à une spécialité.

**Description :**  
Donne les informations détaillé utiles relatif à la substance

**Champs :**

| Champ | Type | Description |
|------|------|-------------|
| nomSubstance | String | Nom de la substance. |
| typeSubstance | String? | Type de la substance (ex. chimique, biologique, etc.). Peut être absent. |
| formuleMoleculaire | String? | Formule moléculaire de la substance. Peut être absente. |
| poidMoleculaire | String? | Poids moléculaire de la substance. Peut être absent. |

**Exemple JSON**
```json
{
    "nomSubstance": "THIOCOLCHICOSIDE",
    "typeSubstance": "chemical",
    "formuleMoleculaire": "C27H33NO10S",
    "poidMoleculaire": 563.62
}
```

### MedicamentStatusCis

**Usage principal :**  
Utilisé pour connaître le statut réglementaire d’une spécialité pharmaceutique (commercialisée, archivée, suspendue, …).

**Description :**  
Utilisé pour connaître le statut réglementaire d’une spécialité pharmaceutique (commercialisée, archivée, suspendue, …).

**Champs :**

| Champ | Type | Description |
|------|------|-------------|
| cis | Int | Code identifiant de la spécialité pharmaceutique (code CIS ANSM). |
| dateEffet | LocalDate? | Date d’effet de l’événement associé à la spécialité. Peut être absente. |
| typeEvenement | String? | Type d’événement réglementaire associé à la spécialité. Peut être absent. |


**Exemple JSON**
```json
{
    "cis": 68268147,
    "dateEffet": "2000-12-28",
    "typeEvenement": "Autorisation"
}
```

### MedicamentStatusCip

**Usage principal :**  
Utilisé pour connaître le statut de commercialisation d’une présentation pharmaceutique (boîte, dosage, conditionnement).

**Description :**  
Décrit un événement de changement de statut d’une présentation pharmaceutique
(par exemple : arrêt de commercialisation d’un conditionnement spécifique.

**Champs :**

| Champ | Type | Description |
|------|------|-------------|
| cip13 | String | Code identifiant de la présentation pharmaceutique (code CIP ANSM). |
| dateEffet | LocalDate? | Date d’effet de l’événement associé à la spécialité. Peut être absente. |
| typeEvenement | String? | Type d’événement réglementaire associé à la spécialité. Peut être absent. |


**Exemple JSON**
```json
{
  "cip13": "3400921659696",
  "dateEffet": "2012-02-15",
  "typeEvenement": "Déclaration de commercialisation"
}
```

# Documentation - Déploiement 

Cette documentation concerne le déploiement du serveur et de la base de donnée via docker sur un VPS ubuntu.

### Installation de Docker

#### 1. Mise à jour du système
Commencez par mettre à jour les paquets système :
```bash
sudo apt update
sudo apt upgrade
```

#### 2. Installation des paquets prérequis
```bash
sudo apt-get install curl apt-transport-https ca-certificates software-properties-common
```

#### 3. Ajout de la clé GPG et du dépôt Docker
```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc
EOF
```

#### 4. Installation de Docker
Mettez à jour les informations du dépôt :
```bash
sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Vérifiez l'installation :
```bash
docker --version
```

#### 5. Démarrage de Docker
Vérifiez le statut et démarrez Docker si nécessaire :
```bash
sudo systemctl status docker
sudo systemctl start docker
sudo systemctl enable docker
```

Démarrage du socket Docker :
```bash
sudo systemctl enable docker.socket
sudo systemctl start docker.socket
```

### Déploiement du projet

#### 1. Clonage du dépôt
```bash
git clone https://github.com/<your-user>/medapp.git
cd medapp/server
```

#### 2. Configuration
Copiez `.env.example` vers `.env` et renseignez les mots de passe (lettres, chiffres et `_ . @ + -` uniquement) :
`MARIADB_ROOT_PASSWORD` (utilisé uniquement par le conteneur MariaDB), `MARIADB_USER`/`MARIADB_PASSWORD`
(compte des jobs d'import) et `API_DB_USER`/`API_DB_PASSWORD` (compte de l'API, lecture seule sur `med_db_views`).
Les comptes sont créés au premier démarrage de MariaDB par `infra/sql/prod/02_create_db_users.sh`.

#### 3. Lancement de l'application
Exécutez les commandes suivantes dans l'ordre :
```bash
# Construction des images Docker
docker compose build

# Démarrage de la base de données
docker compose up -d mariadb

# Import des référentiels
docker compose run --rm server import-referentiels

# Démarrage du serveur
docker compose up -d server
```

