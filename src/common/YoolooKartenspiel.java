// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add history of change

package common;

import utils.HasLogger;
import utils.Statics;

import java.util.ArrayList;


public class YoolooKartenspiel implements HasLogger {


    protected YoolooKarte[][] spielkarten;
    protected int anzahlFarben = YoolooKartenspiel.Kartenfarbe.values().length;
    protected int anzahlWerte = Statics.maxKartenWert;
    ArrayList<YoolooSpieler> spielerliste = new ArrayList<YoolooSpieler>();
    private String Spielname = "Yooloo";

    /**
     * Erstellen einer neuen Spielumgebung Definition des Spielnamens der
     * Spielkarten
     */
    public YoolooKartenspiel() {

        setSpielname("Yooloo" + System.currentTimeMillis());
        getLogger().info("[YoolooKartenSpiel] Spielname: " + getSpielname()); // TODO evtl loeschen

        spielerliste.clear();
        spielkarten = new YoolooKarte[anzahlFarben][anzahlWerte];

        for (int farbe = 0; farbe < anzahlFarben; farbe++) {
            for (int wert = 0; wert < anzahlWerte; wert++) {
                spielkarten[farbe][wert] = new YoolooKarte(Kartenfarbe.values()[farbe], (wert + 1));
            }
        }
        getLogger().info("Je " + anzahlWerte + " Spielkarten fuer " + anzahlFarben + " Spieler zeugt");
    }

    public void listeSpielstand() {
        if (spielerliste.isEmpty()) {
            getLogger().info("(Noch) Keine Spieler registriert");
        } else {
            for (YoolooSpieler yoolooSpieler : spielerliste) {
                getLogger().info(yoolooSpieler.toString());
            }
        }

    }

    /**
     * Luesst einen neuen Spieler an dem Spiel teilnehmen /Fuer lokale Simulationsmit
     * mit Zuordnung der Farbe
     *
     * @param name
     * @return
     */
    public YoolooSpieler spielerRegistrieren(String name) {
        YoolooSpieler neuerSpieler = new YoolooSpieler(name, Statics.maxKartenWert);
        Kartenfarbe[] farben = Kartenfarbe.values();
        neuerSpieler.setSpielfarbe(farben[spielerliste.size()]);
        YoolooKarte[] spielerkarten = spielkarten[spielerliste.size()];
        neuerSpieler.setAktuelleSortierung(spielerkarten);
        this.spielerliste.add(neuerSpieler);
        getLogger().info("Debug; Spieler " + name + " registriert als : " + neuerSpieler);
        return neuerSpieler;
    }

    /**
     * Luesst ein YoolooSpieler aus ClientServer Variante an dem Spiel teilnehmen Der
     * Spieler erhuelt die Farbe korrespondierend zur ClientHandlerID
     *
     * @param neuerSpieler
     * @return
     */
    public YoolooSpieler spielerRegistrieren(YoolooSpieler neuerSpieler) {
        Kartenfarbe[] farben = Kartenfarbe.values();
        neuerSpieler.setSpielfarbe(farben[neuerSpieler.getClientHandlerId()]);
        YoolooKarte[] kartenDesSpielers = spielkarten[neuerSpieler.getClientHandlerId()];
        neuerSpieler.setAktuelleSortierung(kartenDesSpielers);
        this.spielerliste.add(neuerSpieler); // nur fuer Simulation noetig!
        getLogger().info("Debug; Spielerobject registriert als : " + neuerSpieler);
        return neuerSpieler;
    }

    @Override
    public String toString() {
        return "YoolooKartenspiel [anzahlFarben=" + anzahlFarben + ", anzahlWerte=" + anzahlWerte + ", getSpielname()="
                + getSpielname() + "]";
    }

    // nur fuer Simulation / local
    public void spielerSortierungFestlegen() {
        for (int i = 0; i < spielerliste.size(); i++) {
            spielerliste.get(i).sortierungFestlegen();
        }
    }

    // nur fuer Simulation / local
    public void spieleRunden() {
        // Schleife ueber Anzahl der Karten
        for (int i = 0; i < anzahlWerte; i++) {
            getLogger().info("Runde " + (i + 1));
            // Schleife ueber Anzahl der Spieler
            YoolooKarte[] stich = new YoolooKarte[spielerliste.size()];

            for (int j = 0; j < spielerliste.size(); j++) {
                YoolooKarte aktuelleKarte = spielerliste.get(j).getAktuelleSortierung()[i];
                stich[j] = aktuelleKarte;
                getLogger().info(spielerliste.get(j).getName() + " spielt " + aktuelleKarte.toString());
            }
            int stichgewinner = berechneGewinnerIndex(stich);
            if (stichgewinner >= 0) {
                spielerliste.get(stichgewinner).erhaeltPunkte(i + 1);
            }
        }
    }

    public int berechneGewinnerIndexV1_Buggy(YoolooKarte[] karten) {

        int limitWert = Statics.maxKartenWert + 1;
        int maxWert = 0;
        int anzahlKartenMitMaxWert = 0;
        for (int i = 0; i < karten.length; i++) {
            getLogger().info(i + ":" + karten[i].getWert() + " ");
        }
        getLogger().info("");
        while (anzahlKartenMitMaxWert != 1) {
            maxWert = 0;
            for (int i = 0; i < karten.length; i++) {
                YoolooKarte yoolooKarte = karten[i];
                if (maxWert < yoolooKarte.getWert() && yoolooKarte.getWert() < limitWert) {
                    maxWert = yoolooKarte.getWert();
                }
            }
            anzahlKartenMitMaxWert = 0;
            for (int i = 0; i < karten.length; i++) {
                YoolooKarte yoolooKarte = karten[i];
                if (maxWert == yoolooKarte.getWert()) {
                    anzahlKartenMitMaxWert++;
                    limitWert = yoolooKarte.getWert();
                }
            }
            if (limitWert == 0 && anzahlKartenMitMaxWert == 0) {
                return -1;
            }
        }
        int gewinnerIndex = -1;
        for (int i = 0; i < karten.length; i++) {
            YoolooKarte yoolooKarte = karten[i];
            if (yoolooKarte.getWert() == maxWert) {
                gewinnerIndex = i;
            }
        }
        return gewinnerIndex;
    }

    public int berechneGewinnerIndex(YoolooKarte[] karten) {
        int maxwert = 0;
        for (int i = 0; i < karten.length; i++) {
            getLogger().info(i + ":" + karten[i].getWert() + " ");
            if (maxwert < karten[i].getWert())
                maxwert = karten[i].getWert();
        }
        int gewinnerIndex = -1;
        while (maxwert > 0) {
            int anzahlKartenMitMaxwert = 0;
            for (int i = 0; i < karten.length; i++) {
                if (karten[i].getWert() == maxwert) {
                    anzahlKartenMitMaxwert++;
                    gewinnerIndex = i;
                }
            }
            if (anzahlKartenMitMaxwert != 1) {
                maxwert--;
                gewinnerIndex = -1;

            } else {
                getLogger().info("gewinnerIndex: " + gewinnerIndex);
                return gewinnerIndex;
            }
        }
        getLogger().info("Kein gewinnerIndex: ermittelt" + gewinnerIndex);
        return gewinnerIndex;
    }

    public String getSpielname() {
        return Spielname;
    }

    public void setSpielname(String spielname) {
        Spielname = spielname;
    }

    public enum Kartenfarbe {
        Gelb, Rot, Gruen, Blau, Orange, Pink, Violett, Tuerkis
    }


}
