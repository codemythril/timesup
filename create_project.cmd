@echo off
echo Erstelle Projektstruktur...

:: Hauptverzeichnisse
mkdir src\main\java\de\timetracker\ui\components
mkdir src\main\java\de\timetracker\model
mkdir src\main\java\de\timetracker\database
mkdir src\test\java\de\timetracker
mkdir src\main\resources
mkdir src\test\resources

:: Leere Java-Dateien
type nul > src\main\java\de\timetracker\ui\components\AutoCompleteTextField.java
type nul > src\main\java\de\timetracker\model\ActivityDescription.java
type nul > src\main\java\de\timetracker\database\TimeEntryDAO.java

:: Build-Konfiguration
type nul > pom.xml
type nul > .gitignore

echo Projektstruktur erfolgreich erstellt! 