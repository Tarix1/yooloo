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

    public YoolooSpieler(String name, int maxKartenWert) {
        this.name = name;
        this.punkte = 0;
        this.spielfarbe = null;
        this.aktuelleSortierung = new YoolooKarte[maxKartenWert];
    }

    public void sortierungFestlegen() {
        YoolooKarte[] neueSortierung = new YoolooKarte[this.aktuelleSortierung.length];
        int[] kartenWerte = new int[this.aktuelleSortierung.length];

        for (int i = kartenWerte.length; i > 0; i--) {

            int min;
            if (i > 7) {
                min = 6;
            } else {
                min = 0;
            }

            boolean fieldFilled = false;
            while (!fieldFilled) {
                //Wähle ein zufälliges Feld (innerhalb der Range 7-10 oder 1-10) und wenn das Feld frei ist, füge ein
                int randomField = (int) ((Math.random() * (kartenWerte.length - min)) + min);
                if (!(kartenWerte[randomField] > 0)) {
                    kartenWerte[randomField] = i;
                    fieldFilled = true;
                    //getLogger().info("fieldFilled: " + true);
                }
            }
        }
        for (int i = 0; i < this.aktuelleSortierung.length; i++) {
            getLogger().info(kartenWerte[i] + " | ");
            // getLogger.info(kartenWerte[i] + " | ");
            aktuelleSortierung[i].setWert(kartenWerte[i]);
        }
        // aktuelleSortierung[5].setWert(5);
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

}
