# Pbil Client

Fabric-Client-Mod für Minecraft 1.21.11 mit Modul-Menü.

## Bedienung

- **P**: Menü öffnen (Esc schließt es)
- **Linksklick** auf ein Modul: an/aus
- **Mittelklick** auf ein Modul: Taste zuweisen (Esc beim Zuweisen = Taste löschen)
- **Scrollrad** über Zoom, Block ESP, Custom FOV: Wert ändern
- **Tippen** im Menü: Module suchen
- **C** gedrückt halten: Zoom (wenn das Modul an ist)

Einstellungen werden in `config/pbil-client.properties` gespeichert.

## Module

| Kategorie | Module |
|-----------|--------|
| Render    | Fullbright, Zoom, ESP, Item ESP, Block ESP, Storage ESP |
| Visuals   | Custom FOV |
| Client    | HUD, Performance |
| Komfort   | Sprint |

## Bauen

Voraussetzungen: JDK 21.

```
gradle wrapper --gradle-version 9.2.1
./gradlew build
```

Die fertige Mod liegt danach in `build/libs/pbil-client-1.0.0.jar`.
Ins `mods`-Verzeichnis legen, zusammen mit Fabric Loader und Fabric API.
Zum Entwickeln das Projekt in IntelliJ als Gradle-Projekt öffnen und `./gradlew runClient` starten.

## Hinweis

`EspRenderer.java` nutzt die Render-API, die sich bei Minecraft-Versionen oft ändert.
Falls dort beim Bauen etwas nicht passt, ist das die einzige Datei, die angepasst werden muss.
