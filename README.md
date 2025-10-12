# Analyse statique Java (JDT AST ) & Graphe d’appel (GraphStream)

Ce projet analyse un code Java (JDK **21**) avec **Eclipse JDT Core** pour construire l’**AST** et le **Java Model**, calcule des **métriques** (classes, méthodes, attributs, LOC, top 10%), et génère un **graphe d’appel** visualisé via **GraphStream** (UI **Swing**).

---

##  Fonctionnalités
- Parsing Java via **JDT** (AST JLS21 + **bindings** activés).
- **Métriques** globales et détaillées : nb classes/méthodes/packages/LOC, moyennes, top 10%, > X méthodes, etc.
- **Graphe d’appel** filtré pour ne montrer que les packages du projet (exclusion `java.*`, `org.graphstream.*`, ...).
- **Interface Swing** : onglet **Statistiques** (tableaux) + onglet **Graphe** (zoom, fit, labels).

---

##  Prérequis
- **JDK 21** (Project SDK et Module SDK configurés sur 21 dans l’IDE)
- **Maven 3.9+**
- OS : Windows

Vérifier les versions :
```bash
java -version
mvn -v
```
## Installation
Cloner le dépôt et entrer dans le dossier du projet :
```bash
git clone <VOTRE_REPO_GIT_URL> tp-analyse-statique
cd tp-analyse-statique
```
## Build
Compilation + packaging Maven :
```bash
mvn -U clean package
```
> Le build peut produire un **fat-jar** `*-jar-with-dependencies.jar` si le `maven-assembly-plugin` est configuré avec la bonne `Main-Class` (par ex. `org.example.Main`).

---
## Interface utilisateur
- **Statistiques** :  
  Tableaux récapitulatifs (totaux, moyennes), listes **Top 10%** par méthodes/attributs, **intersection** et **Top 10% méthodes par classe** (LOC).

- **Graphe d’appel** :  
  Visualisation GraphStream avec toolbar : **Zoom +/−**, **Fit**, **Labels**. Les packages externes (`java.*`, `javax.*`, `jdk.*`, `org.eclipse.*`, `org.graphstream.*`) sont filtrés pour rester centré sur votre projet.

---

## Structure (packages)
```
model/     -> ClassInfo, MethodInfo (modèle des entités)
parser/    -> JdtProject (ASTParser JLS21 + bindings), EntityCollector (ASTVisitor)
stats/     -> MetricService (calculs & agrégations)
graph/     -> CallGraphBuilder (extraction des appels), CallGraphToGraphStream (construction du graphe)
ui/        -> MetricsPanel (tables), GraphPanel (SwingViewer avec zoom/fit/labels)
utils/     -> Locs (LOC non vides), Args (parse des arguments)
org.example.Main -> point d’entrée (Swing + onglets)
```

---
