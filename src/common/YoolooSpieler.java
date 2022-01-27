// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add history of change

package common;

import common.YoolooKartenspiel.Kartenfarbe;
import utils.HasLogger;

import java.io.Serializable;
import java.util.Arrays;

public class YoolooSpieler implements Serializable, HasLogger {

    private static final long serialVersionUID = 376078630788146549L;
    private String name;
    private Kartenfarbe spielfarbe;
    private int clientHandlerId = -1;
    private int punkte;
    private YoolooKarte[] aktuelleSortierung;
    private Boolean loggedIn;

    public YoolooSpieler(String name, int maxKartenWert) {
        this.name = name;
        this.punkte = 0;
        this.spielfarbe = null;
        this.aktuelleSortierung = new YoolooKarte[maxKartenWert];
    }

    public YoolooSpieler(String name, YoolooKarte[] alteSortierung) {
        this.name = name;
        this.punkte = 0;
        this.spielfarbe = null;
        this.aktuelleSortierung = alteSortierung;
    }

    /**
     * Konstruktor zur erstellung aus den ausgelesenen Spielerdaten
     *
     * @param name
     */
    public YoolooSpieler(String name) {
        this.name = name;
    }

    /**
     * Spielstrategie:
     * Die Karten 10, 9 & 8 werden zuerst auf drei der vier höhsten Positionen (7 - 10) verteilt (ein Feld wird erst später befüllt).
     * Lokales Array "kartenWerte" wird erstellt und nach dem folgendem Prinzip befüllt:
     * ---
     * Für die erste For-Schleife gilt i = Kartenwert -> fängt bei 10 an und zählt runter.
     * Wenn 10 - 8 verteilt wurde ändert sich die Zufalls-Range von 7 - 10 zu 1 - 10 -> restliche Positionen werden gefüllt.
     * Ein Feld wird gewählt -> wenn es frei ist: füge die gegebene Karte i ein.
     * ---
     * Für die zweite For-Schleife gilt i = index für das lokale Array (Normalfall: 0 - 9).
     * Geht Feld für Feld durch das befüllte lokale Array "kartenWerte" und überträgt den Inhalt in "aktuelleSortierung".
     * <p>
     * Zusatz:
     * Wir haben uns dafür entschieden drei verschiedene Strategien zu implementieren, die źufällig ausgewählt werden,
     * damit nicht alle Spieler die gleiche Strategie verfolgen.
     */
    public void sortierungFestlegen() {
        int randomStrat = (int) ((Math.random() * (2 - 0)));
        int[] kartenWerte = new int[this.aktuelleSortierung.length];
        switch (randomStrat) {
            case 0:
                //+#High-Prio-Strat#+
                //i = Karte, startet bei 10 -> setzt zuerst die Position von 10, 9, 8 fest, bevor es die Zufalls-Range ändert
                for (int i = kartenWerte.length; i > 0; i--) {
                    int min;
                    //Legt Karte 10,9 & 8 auf drei der vier Positionen 10,9,8 & 7 (eine zufällige Position bleibt frei)
                    if (i > 7) {
                        min = 6;
                    } else {
                        min = 0;
                    }
                    boolean fieldFilled = false;
                    while (!fieldFilled) {
                        //Wähle ein zufälliges Feld (innerhalb der Range 7-10 oder 1-10) und wenn das Feld frei ist -> füge ein
                        int randomField = (int) ((Math.random() * (kartenWerte.length - min)) + min);
                        if (!(kartenWerte[randomField] > 0)) {
                            kartenWerte[randomField] = i;
                            fieldFilled = true;
                            //getLogger().info("fieldFilled: " + true);
                        }
                    }
                }
                break;
            case 1:
                //+#Full-Random-Strat#+
                YoolooKarte[] neueSortierung = new YoolooKarte[this.aktuelleSortierung.length];
                for (int i = 0; i < neueSortierung.length; i++) {
                    int neuerIndex = (int) (Math.random() * neueSortierung.length);
                    while (neueSortierung[neuerIndex] != null) {
                        neuerIndex = (int) (Math.random() * neueSortierung.length);
                    }
                    neueSortierung[neuerIndex] = aktuelleSortierung[i];
                    // System.out.println(i+ ". neuerIndex: "+neuerIndex);
                }
                aktuelleSortierung = neueSortierung;
                break;
            case 2:
                //+#No-10-Strat#+
                //i = Karte, startet bei 10 -> setzt zuerst die Position von 10, 9, 8 fest, bevor es die Zufalls-Range ändert
                for (int i = (kartenWerte.length); i > 0; i--) {
                    int min = 0;
                    int max = 8;
                    //Legt Karte 10,9 & 8 auf drei der vier Positionen 10,9,8 & 7 (eine zufällige Position bleibt frei)
                    boolean fieldFilled = false;
                    while (!fieldFilled) {
                        //Wähle ein zufälliges Feld (innerhalb der Range 7-10 oder 1-10) und wenn das Feld frei ist -> füge ein

                        int randomField = (int) ((Math.random() * (max - min)) + min);
                        if (i == 1) {
                            randomField = 9;
                        }
                        if (!(kartenWerte[randomField] > 0)) {
                            kartenWerte[randomField] = i;
                            fieldFilled = true;
                            //getLogger().info("fieldFilled: " + true);
                        }
                    }
                }
        }
        //i = Index 0 bis max. Index des generierte Arrays "kartenWerte". Überschreibt Feld für Feld die „aktuelleSortierung“ des/der gegebenen Spielers/in. Spielstrategie ENDE
        if (randomStrat != 1) {
            for (int i = 0; i < this.aktuelleSortierung.length; i++) {
                getLogger().info(kartenWerte[i] + " | ");
                // getLogger.info(kartenWerte[i] + " | ");
                aktuelleSortierung[i].setWert(kartenWerte[i]);
            }
        }
    }

    public int erhaeltPunkte(int neuePunkte) {
        getLogger().info(name + " hat " + punkte + " P - erhaelt " + neuePunkte + " P - neue Summe: ");
        this.punkte = this.punkte + neuePunkte;
        getLogger().info("" + this.punkte);
        return this.punkte;
    }

    @Override
    public String toString() {
        return "YoolooSpieler [name=" + name + ", spielfarbe=" + spielfarbe + ", puntke=" + punkte
                + ", altuelleSortierung=" + Arrays.toString(aktuelleSortierung) + "]";
    }


    /**
     * Modified to return a csv representation of the player
     *
     * @return csv String
     */
    public String toCSVString() {
        String csvString = this.name + ";" + this.loggedIn + ";";

        return csvString;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Kartenfarbe getSpielfarbe() {
        return spielfarbe;
    }

    public void setSpielfarbe(Kartenfarbe spielfarbe) {
        this.spielfarbe = spielfarbe;
    }

    public int getClientHandlerId() {
        return clientHandlerId;
    }

    public void setClientHandlerId(int clientHandlerId) {
        this.clientHandlerId = clientHandlerId;
    }

    public int getPunkte() {
        return punkte;
    }

    public void setPunkte(int puntke) {
        this.punkte = puntke;
    }

    public YoolooKarte[] getAktuelleSortierung() {
        return aktuelleSortierung;
    }

    public void setAktuelleSortierung(YoolooKarte[] aktuelleSortierung) {
        this.aktuelleSortierung = aktuelleSortierung;
    }

    public void stichAuswerten(YoolooStich stich) {
        getLogger().info(stich.toString());

    }

    public Boolean getLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(Boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

}
