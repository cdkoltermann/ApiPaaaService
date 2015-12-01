---

![TU Dortmund Logo](http://www.ub.tu-dortmund.de/images/tu-logo.png) 

![UB Dortmund Logo](http://www.ub.tu-dortmund.de/images/ub-schriftzug.jpg)

---

# PaaaService - Eine Implementierung der PAAA-Spezifikation

Die UB Dortmund benötigt einen nach aussen vom Bibliothekssystem (ILS) unabhängigen Service für Kontofunktionen aus Administrationssicht. Zu diesen zählen: 

* administrative Aufgaben wie Kontopflege (also neues Konto anlegen, Konto verändern) sowie "Sanktionen" wie Konto sperren und entsperren, aber auch das Buchen von Gebühren.
* Abrufen von speziellen Kontoinformationen

Die Notwendigkeit für einen solchen Service ergibt sich aus einer Vielzahl von neuen vernetzten Dienstleistungen innerhalb der TU Dortmund. Zwei Szenarien sind dabei zu berücksichtigen:

* **Szenario 1:** Für TU-Angehörige liefert das TU-IDM als Mastersystem mittels eines OpenLDAP die Daten, welche dann in das ILS fließen.
* **Szenario 2:** Für die TU-externen Bibliotheksbenutzer werden in klassischer Weise Konten via Ausleih-Client bzw. OPAC-Selbstanmeldung des ILS erstellt. 

Das zweite Szenario spaltet sich weiter auf. Im Rahmen der Universitätsallianz Ruhr (UA-Ruhr bzw. UA-R) sollen die Daten der Angehörigen der in diesem Konsortium beteiligten Hochschulen Duisburg-Essen und Bochum ebenfalls automatisch zum Anlegen und Pflegen eines Bibliothekskontos herangezogen werden. 

Die Authentifizierungs- und Autorisierungsfunktionen werden mittels OAuth 2.0 realisiert.

## Anwendung

PaaaService ist in Java 1.8 implementiert und stellt drei Interfaces für lokale Anpassungen zur Verfügung.

* `de.tu_dortmund.ub.api.paia.core.ils.IntegratedLibrarySystem` zur Implementierung der Anbindung an ein ILS
* `de.tu_dortmund.ub.api.paia.auth.AuthorizationInterface` zur Implementierung der Anbindung an einen OAuth-Token-Endpoint

Die Konfiguration der Implementierung geschieht mittels `META-INF.service`.

## Kontakt

**api@ubdo - Application Programming Interfaces der Universitätsbibliothek Dortmund**

Technische Universität Dortmund // Universitätsbibliothek // Bibliotheks-IT // Vogelpothsweg 76 // 44227 Dortmund

[Webseite](https://api.ub.tu-dortmund.de) // [E-Mail](mailto:api@ub.tu-dortmund.de)

---

![Creative Commons License](http://i.creativecommons.org/l/by/4.0/88x31.png)

This work is licensed under a [Creative Commons Attribution 4.0 International License (CC BY 4.0)](http://creativecommons.org/licenses/by/4.0/)

--- 
