// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add  start_Client() SERVERMESSAGE_CHANGE_STATE 

package client;

import common.LoginMessage;
import common.YoolooSpieler;
import common.YoolooStich;
import messages.ClientMessage;
import messages.ClientMessage.ClientMessageType;
import messages.ServerMessage;
import utils.HasLogger;
import utils.Statics;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;

public class YoolooClient implements HasLogger {

    private final String serverHostname = "localhost";
    private final String spielerName = "Name" + (System.currentTimeMillis() + "").substring(6);
    private int serverPort = 44137;
    private Socket serverSocket = null;
    private ObjectInputStream ois = null;
    private ObjectOutputStream oos = null;
    private ClientState clientState = ClientState.CLIENTSTATE_NULL;
    private LoginMessage newLogin = null;
    private YoolooSpieler meinSpieler;
    private YoolooStich[] spielVerlauf = null;

    public YoolooClient() {
        super();
    }

    public YoolooClient(String serverHostname, int serverPort) {
        super();
        this.serverPort = serverPort;
        clientState = ClientState.CLIENTSTATE_NULL;
    }

    /**
     * Client arbeitet statusorientiert als Kommandoempfuenger in einer Schleife.
     * Diese terminiert wenn das Spiel oder die Verbindung beendet wird.
     *
     * Ohne hinzugefügte try catch Blöcke läuft die while-Schleife unendlich weiter bei einem Fehler
     */
    public void startClient() {

        try {
            clientState = ClientState.CLIENTSTATE_CONNECT;
            verbindeZumServer();

            //this.logInServer();

            while (clientState != ClientState.CLIENTSTATE_DISCONNECTED && ois != null && oos != null) {
                // 1. Schritt Kommado empfangen
                ServerMessage kommandoMessage = empfangeKommando();
                getLogger().info("[id-x]ClientStatus: " + clientState + "] " + kommandoMessage.toString());
                // 2. Schritt ClientState ggfs aktualisieren (fuer alle neuen Kommandos)
                ClientState newClientState = kommandoMessage.getNextClientState();
                if (newClientState != null) {
                    clientState = newClientState;
                }
                // 3. Schritt Kommandospezifisch reagieren
                switch (kommandoMessage.getServerMessageType()) {
                    case SERVERMESSAGE_SENDLOGIN:
                        try {
                            // Server fordert Useridentifikation an
                            // Falls User local noch nicht bekannt wird er bestimmt
                            if (newLogin == null || clientState == ClientState.CLIENTSTATE_LOGIN) {
                                // TODO Klasse LoginMessage erweiteren um Interaktives ermitteln des
                                // Spielernames, GameModes, ...)
                                newLogin = eingabeSpielerDatenFuerLogin(); //Dummy aufruf
                                newLogin = new LoginMessage(spielerName);
                            }
                            // Client meldet den Spieler an den Server
                            oos.writeObject(newLogin);
                            getLogger().info("[id-x]ClientStatus: " + clientState + "] : LoginMessage fuer  " + spielerName
                                    + " an server gesendet warte auf Spielerdaten");
                            empfangeSpieler();
                            // ausgabeKartenSet();
                        } catch (Exception e) {
                            getLogger().log(Level.SEVERE, "Login konnte nicht durchgeführt werden", e);
                            clientState = ClientState.CLIENTSTATE_DISCONNECTED;
                        }
                        break;
                    case SERVERMESSAGE_SORT_CARD_SET:
                        try {
                            // sortieren Karten
                            meinSpieler.sortierungFestlegen();
                            ausgabeKartenSet();
                            // ggfs. Spielverlauf löschen
                            spielVerlauf = new YoolooStich[Statics.maxKartenWert];
                            ClientMessage message = new ClientMessage(ClientMessageType.ClientMessage_OK,
                                    "Kartensortierung ist erfolgt!");
                            oos.writeObject(message);
                        } catch (Exception e) {
                            getLogger().log(Level.SEVERE, "Karten konnten nicht sortiert werden", e);
                            clientState = ClientState.CLIENTSTATE_DISCONNECTED;
                        }
                        break;
                    case SERVERMESSAGE_SEND_CARD:
                        try {
                            spieleStich(kommandoMessage.getParamInt());
                        } catch (Exception e) {
                            getLogger().log(Level.SEVERE, "Senden der Karte ist fehlgeschlagen", e);
                            clientState = ClientState.CLIENTSTATE_DISCONNECTED;
                        }
                        break;
                    case SERVERMESSAGE_RESULT_SET:
                        try {
                            getLogger().info("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState
                                    + "] : Ergebnis ausgeben ");
                            String ergebnis = empfangeErgebnis();
                            getLogger().info(ergebnis);
                        } catch (Exception e) {
                            getLogger().log(Level.SEVERE, "Ergebnis konnte nicht gesetzt werden", e);
                            clientState = ClientState.CLIENTSTATE_DISCONNECTED;
                        }
                        break;
                    // basic version: wechsel zu ClientState Disconnected thread beenden
                    case SERVERMESSAGE_CHANGE_STATE:
                        break;

                    default:
                        break;
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "", e);
        }
    }

    /**
     * Verbindung zum Server aufbauen, wenn Server nicht antwortet nach ein Sekunde
     * nochmals versuchen
     *
     * @throws UnknownHostException
     * @throws IOException
     */
    // TODO Abbruch nach x Minuten einrichten
    private void verbindeZumServer() throws UnknownHostException, IOException {
        while (serverSocket == null) {
            try {
                serverSocket = new Socket(serverHostname, serverPort);
            } catch (ConnectException e) {
                getLogger().info("Server antwortet nicht - ggfs. neu starten");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                }
            }
        }
        getLogger().info("[Client] Serversocket eingerichtet: " + serverSocket.toString());
        // Kommunikationskanuele einrichten
        ois = new ObjectInputStream(serverSocket.getInputStream());
        oos = new ObjectOutputStream(serverSocket.getOutputStream());
    }

    private void logInServer() throws Exception {
        oos.writeObject(this.spielerName);
    }

    private void spieleStich(int stichNummer) throws IOException {
        getLogger().info("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState
                + "] : Spiele Karte " + stichNummer);
        spieleKarteAus(stichNummer);
        YoolooStich iStich = empfangeStich();
        spielVerlauf[stichNummer] = iStich;
        getLogger().info("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState
                + "] : Empfange Stich " + iStich);
        if (iStich.getSpielerNummer() == meinSpieler.getClientHandlerId()) {
            getLogger().info(
                    "[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState + "] : Gewonnen - ");
            meinSpieler.erhaeltPunkte(iStich.getStichNummer() + 1);
        }

    }

    private void spieleKarteAus(int i) throws IOException {
        oos.writeObject(meinSpieler.getAktuelleSortierung()[i]);
    }

    // Methoden fuer Datenempfang vom Server / ClientHandler
    private ServerMessage empfangeKommando() {
        ServerMessage kommando = null;
        boolean failed = false;
        try {
            kommando = (ServerMessage) ois.readObject();
        } catch (Exception e) {
            failed = true;
            getLogger().log(Level.SEVERE, "Kommando nicht empfangen", e);
        }
        if (failed)
            kommando = null;
        return kommando;
    }

    private void empfangeSpieler() throws ClassCastException {
        try {
            meinSpieler = (YoolooSpieler) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            getLogger().log(Level.SEVERE, "Spieler konnte nicht empfangen werden", e);
        }
    }

    private YoolooStich empfangeStich() {
        try {
            return (YoolooStich) ois.readObject();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Stich konnte nicht empfangen werden", e);
        }
        return null;
    }

    private String empfangeErgebnis() {
        try {
            return (String) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            getLogger().log(Level.SEVERE, "Ergebnis konnte nicht empfangen werden", e);
        }
        return null;
    }

    private LoginMessage eingabeSpielerDatenFuerLogin() {
        // TODO Spielername, GameMode und ggfs mehr ermitteln
        return null;
    }

    public void ausgabeKartenSet() {
        // Ausgabe Kartenset
        getLogger().info("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState
                + "] : Uebermittelte Kartensortierung beim Login ");
        for (int i = 0; i < meinSpieler.getAktuelleSortierung().length; i++) {
            getLogger().info("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState
                    + "] : Karte " + (i + 1) + ":" + meinSpieler.getAktuelleSortierung()[i]);
        }

    }

    public enum ClientState {
        CLIENTSTATE_NULL, // Status nicht definiert
        CLIENTSTATE_CONNECT, // Verbindung zum Server wird aufgebaut
        CLIENTSTATE_LOGIN, // Anmeldung am Client Informationen des Users sammeln
        CLIENTSTATE_RECEIVE_CARDS, // Anmeldung am Server
        CLIENTSTATE_SORT_CARDS, // Anmeldung am Server
        CLIENTSTATE_REGISTER, // t.b.d.
        CLIENTSTATE_PLAY_SINGLE_GAME, // Spielmodus einfaches Spiel
        CLIENTSTATE_DISCONNECT, // Verbindung soll getrennt werden
        CLIENTSTATE_DISCONNECTED // Vebindung wurde getrennt
    }

}
