// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    130 | change ServerMessageType.SERVERMESSAGE_RESULT_SET to SERVERMESSAGE_RESULT_SET200107| cic |    130 | change ServerMessageType.SERVERMESSAGE_RESULT_SET to SERVERMESSAGE_RESULT_SET
//  V0.106  |      | cic |        | change empfangeVomClient(this.ois) to empfangeVomClient()


package server;

import client.YoolooClient.ClientState;
import common.LoginMessage;
import common.YoolooKarte;
import common.YoolooSpieler;
import common.YoolooStich;
import messages.ClientMessage;
import messages.ServerMessage;
import messages.ServerMessage.ServerMessageResult;
import messages.ServerMessage.ServerMessageType;
import utils.HasLogger;
import utils.Statics;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class YoolooClientHandler extends Thread implements HasLogger {

    private final static int delay = 100;

    private YoolooServer myServer;
    private Socket clientSocket;
    private SocketAddress socketAddress = null;
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;

    private ServerState state;
    private YoolooSession session;
    private YoolooSpieler meinSpieler = null;
    private int clientHandlerId;

    private Map<Integer, List<YoolooKarte>> YoolooSpielerkartenMap = new HashMap<>();

    public YoolooClientHandler(YoolooServer yoolooServer, Socket clientSocket) {
        this.myServer = yoolooServer;
        myServer.toString();
        this.clientSocket = clientSocket;
        this.state = ServerState.ServerState_NULL;
    }

    /**
     * Serverseitige Steuerung des Clients
     */
    @Override
    public void run() {
        try {
            state = ServerState.ServerState_CONNECT; // Verbindung zum Client aufbauen
            verbindeZumClient();

            state = ServerState.ServerState_REGISTER; // Abfragen der YoolooSpieler LoginMessage
            sendeKommando(ServerMessageType.SERVERMESSAGE_SENDLOGIN, ClientState.CLIENTSTATE_LOGIN, null);

            Object antwortObject = null;
            while (this.state != ServerState.ServerState_DISCONNECTED) {
                // Empfange YoolooSpieler als Antwort vom Client
                antwortObject = empfangeVomClient();
                if (antwortObject instanceof ClientMessage) {
                    ClientMessage message = (ClientMessage) antwortObject;
                    getLogger().info("[ClientHandler" + clientHandlerId + "] Nachricht Vom Client: " + message);
                }
                switch (state) {
                    case ServerState_REGISTER:
                        // Neuer YoolooYoolooSpieler in Runde registrieren
                        if (antwortObject instanceof LoginMessage) {
                            LoginMessage newLogin = (LoginMessage) antwortObject;

                            Boolean playerFound = false;
                            Boolean loggedIn = false;
                            YoolooKarte[] aktuelleSortierung = null;
                            for (Map.Entry<String, YoolooSpieler> entry : YoolooServer.PLAYER_LIST.entrySet()) {
                                if (entry.getKey().equals(newLogin.getSpielerName())) {
                                    playerFound = true;
                                    loggedIn = entry.getValue().getLoggedIn();
                                    if (loggedIn && entry.getValue().getAktuelleSortierung() != null) {
                                        getLogger().info("Alte Sortierung übernommen");
                                        aktuelleSortierung = entry.getValue().getAktuelleSortierung();
                                    }
                                }
                            }
                            /**
                             * Je nachdem, ob der Spieler vorhanden und eingeloggt ist wird anders reagiert,
                             * sollte der Spieler bereits vorhanden sein und eingeloggt wird der Thread beendet.
                             */
                            if (!playerFound || !loggedIn) {
                                if (aktuelleSortierung != null)
                                    meinSpieler = new YoolooSpieler(newLogin.getSpielerName(), aktuelleSortierung);
                                else
                                    meinSpieler = new YoolooSpieler(newLogin.getSpielerName(), Statics.maxKartenWert);
                                meinSpieler.setLoggedIn(true);
                                YoolooServer.PLAYER_LIST.put(newLogin.getSpielerName(), meinSpieler);
                                meinSpieler.setClientHandlerId(clientHandlerId);
                                registriereYoolooSpielerInSession(meinSpieler);
                                oos.writeObject(meinSpieler);
                                sendeKommando(ServerMessageType.SERVERMESSAGE_SORT_CARD_SET, ClientState.CLIENTSTATE_SORT_CARDS,
                                        null);
                                this.state = ServerState.ServerState_PLAY_SESSION;
                                break;
                            } else {
                                this.state = ServerState.ServerState_DISCONNECTED;
                                sendeKommando(ServerMessageType.SERVERMESSAGE_CHANGE_STATE, ClientState.CLIENTSTATE_DISCONNECTED, null);
                                getLogger().warning("Name doppelt, beende thread");
                                this.interrupt();
                            }
                        }
                    case ServerState_PLAY_SESSION:
                        switch (session.getGamemode()) {
                            case GAMEMODE_SINGLE_GAME:
                                // Triggersequenz zur Abfrage der einzelnen Karten des YoolooSpielers
                                for (int stichNummer = 0; stichNummer < Statics.maxKartenWert; stichNummer++) {
                                    try {
                                        sendeKommando(ServerMessageType.SERVERMESSAGE_SEND_CARD,
                                                ClientState.CLIENTSTATE_PLAY_SINGLE_GAME, null, stichNummer);
                                        // Neue YoolooKarte in Session ausspielen und Stich abfragen
                                        YoolooKarte neueKarte = (YoolooKarte) empfangeVomClient();
                                        getLogger().info("[ClientHandler" + clientHandlerId + "] Karte empfangen:" + neueKarte);
                                        YoolooStich currentstich = spieleKarte(stichNummer, neueKarte);
                                        // Punkte fuer gespielten Stich ermitteln
                                        if (currentstich != null) {
                                            if (currentstich.getSpielerNummer() == clientHandlerId) {
                                                meinSpieler.erhaeltPunkte(stichNummer + 1);
                                            }
                                            getLogger().info("[ClientHandler" + clientHandlerId + "] Stich " + stichNummer
                                                    + " wird gesendet: " + currentstich);
                                            // Stich an Client uebermitteln
                                            oos.writeObject(currentstich);
                                        } else {
                                            getLogger().warning("Kein Stich erhalten");
                                        }
                                    } catch (Exception e) {
                                        getLogger().log(Level.WARNING, "Stich: [" + stichNummer + "] verlief nicht nach Plan", e);
                                        getLogger().info("Auswertung frühzeitig abgebrochen");
                                        break;
                                    }
                                }
                                this.state = ServerState.ServerState_DISCONNECT;
                                break;
                            default:
                                getLogger().info("[ClientHandler" + clientHandlerId + "] GameMode nicht implementiert");
                                this.state = ServerState.ServerState_DISCONNECT;
                                break;
                        }
                    case ServerState_DISCONNECT:

                        YoolooServer.PLAYER_LIST.forEach((k, v) -> {
                            if (k.equals(meinSpieler.getName())) {
                                meinSpieler.setLoggedIn(false);
                                YoolooServer.PLAYER_LIST.put(k, meinSpieler);
                            }

                        });
                        sendeKommando(ServerMessageType.SERVERMESSAGE_CHANGE_STATE, ClientState.CLIENTSTATE_DISCONNECTED, null);
//					sendeKommando(ServerMessageType.SERVERMESSAGE_RESULT_SET, ClientState.CLIENTSTATE_DISCONNECTED,	null);
                        oos.writeObject(session.getErgebnis());
                        this.state = ServerState.ServerState_DISCONNECTED;
                        break;
                    default:
                        getLogger().info("Undefinierter Serverstatus - tue mal nichts!");
                }
            }
            //this.writePlayerList(playerList);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "", e);
        } finally {
            getLogger().info("[ClientHandler" + clientHandlerId + "] Verbindung zu " + socketAddress + " beendet");
        }

    }

    private void sendeKommando(ServerMessageType serverMessageType, ClientState clientState,
                               ServerMessageResult serverMessageResult, int paramInt) throws IOException {
        ServerMessage kommandoMessage = new ServerMessage(serverMessageType, clientState, serverMessageResult,
                paramInt);
        getLogger().info("[ClientHandler" + clientHandlerId + "] Sende Kommando: " + kommandoMessage);
        oos.writeObject(kommandoMessage);
    }

    private void sendeKommando(ServerMessageType serverMessageType, ClientState clientState,
                               ServerMessageResult serverMessageResult) throws IOException {
        ServerMessage kommandoMessage = new ServerMessage(serverMessageType, clientState, serverMessageResult);
        getLogger().info("[ClientHandler" + clientHandlerId + "] Sende Kommando: " + kommandoMessage);
        oos.writeObject(kommandoMessage);
    }

    private void verbindeZumClient() throws IOException {
        oos = new ObjectOutputStream(clientSocket.getOutputStream());
        ois = new ObjectInputStream(clientSocket.getInputStream());
        getLogger().info("[ClientHandler  " + clientHandlerId + "] Starte ClientHandler fuer: "
                + clientSocket.getInetAddress() + ":->" + clientSocket.getPort());
        socketAddress = clientSocket.getRemoteSocketAddress();
        getLogger().info("[ClientHandler" + clientHandlerId + "] Verbindung zu " + socketAddress + " hergestellt");
        oos.flush();
    }

    private Object empfangeVomClient() {
        Object antwortObject;
        try {
            antwortObject = ois.readObject();
            return antwortObject;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Fehler bei Clientanwort", e);
        }
        return null;
    }

    private void registriereYoolooSpielerInSession(YoolooSpieler meinYoolooSpieler) {
        getLogger().info("[ClientHandler" + clientHandlerId + "] registriereYoolooSpielerInSession " + meinYoolooSpieler.getName());
        session.getAktuellesSpiel().spielerRegistrieren(meinYoolooSpieler);
    }

    /**
     * Methode checkt, ob die empfangene Karte eines Spielers in vergangenen Runde bereits gelegt wurde.
     * Wenn dies der Fall ist, wurde die Karte doppelt gespielt. Die doppelt gespielte Karte
     * bekommt den Wert 0, damit der Spieler diese Runde auf jeden Fall verliert.
     * Zudem spielt die Methode eine Karte des Clients in der Session aus und wartet auf die
     * Karten aller anderen Mitspieler.
     * Wenn die empfangene Karte noch nicht gelegt wurde, wird diese in eine Liste geschrieben oder eine neue Liste
     * erstellt, wenn noch keine vorhanden ist.
     * Dann wird das Ergebnis in Form eines Stichs an den Client zurueck zu geben
     *
     * @param stichNummer
     * @param empfangeneKarte
     * @return
     */
    private YoolooStich spieleKarte(int stichNummer, YoolooKarte empfangeneKarte) {
        if (YoolooSpielerkartenMap.get(clientHandlerId) != null) {
            for (YoolooKarte karte : YoolooSpielerkartenMap.get(clientHandlerId)) {
                if (karte.getWert() == empfangeneKarte.getWert() && karte.getFarbe().equals(empfangeneKarte.getFarbe())) {
                    //getLogger.warn(clientHandlerId + " hat versucht die Karte " + empfangeneKarte + "doppelt zu legen");
                    empfangeneKarte.setWert(0);
                    break;
                }
            }
        }

        YoolooStich aktuellerStich = null;
        getLogger().info("[ClientHandler" + clientHandlerId + "] spiele Stich Nr: " + stichNummer
                + " KarteKarte empfangen: " + empfangeneKarte.toString());
        session.spieleKarteAus(clientHandlerId, stichNummer, empfangeneKarte);
        // ausgabeSpielplan(); // Fuer Debuginformationen sinnvoll
        int counter = 0;
        while (aktuellerStich == null && counter < 50) {
            try {
                getLogger().info("[ClientHandler" + clientHandlerId + "] warte " + delay + " ms ");
                counter++;
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                getLogger().log(Level.SEVERE, "Warten auf Client [" + clientHandlerId + "] unterbrochen", e);
            }
            aktuellerStich = session.stichFuerRundeAuswerten(stichNummer);
            if (YoolooSpielerkartenMap.get(clientHandlerId) == null) {
                List<YoolooKarte> kartenNummern = new ArrayList<>();
                kartenNummern.add(empfangeneKarte);
                YoolooSpielerkartenMap.put(clientHandlerId, kartenNummern);
            } else {
                YoolooSpielerkartenMap.get(clientHandlerId).add(empfangeneKarte);
            }

        }
        return aktuellerStich;
    }

    public void setHandlerID(int clientHandlerId) {
        getLogger().info("[ClientHandler" + clientHandlerId + "] clientHandlerId " + clientHandlerId);
        this.clientHandlerId = clientHandlerId;

    }

    public void ausgabeSpielplan() {
        getLogger().info("Aktueller Spielplan");
        for (int i = 0; i < session.getSpielplan().length; i++) {
            for (int j = 0; j < session.getSpielplan()[i].length; j++) {
                getLogger().info("[ClientHandler" + clientHandlerId + "][i]:" + i + " [j]:" + j + " Karte: "
                        + session.getSpielplan()[i][j]);
            }
        }
    }

    /**
     * Gemeinsamer Datenbereich fuer den Austausch zwischen den ClientHandlern.
     * Dieser wird im jedem Clienthandler der Session verankert. Schreibender
     * Zugriff in dieses Object muss threadsicher synchronisiert werden!
     *
     * @param session
     */
    public void joinSession(YoolooSession session) {
        getLogger().info("[ClientHandler" + clientHandlerId + "] joinSession " + session.toString());
        this.session = session;

    }

    /**
     * ClientHandler / Server Sessionstatusdefinition
     */
    public enum ServerState {
        ServerState_NULL, // Server laeuft noch nicht
        ServerState_CONNECT, // Verbindung mit Client aufbauen
        ServerState_LOGIN, // noch nicht genutzt Anmeldung eines registrierten Users
        ServerState_REGISTER, // Registrieren eines YoolooSpielers
        ServerState_MANAGE_SESSION, // noch nicht genutzt Spielkoordination fuer komplexere Modi
        ServerState_PLAY_SESSION, // Einfache Runde ausspielen
        ServerState_DISCONNECT, // Session beendet ausgespielet Resourcen werden freigegeben
        ServerState_DISCONNECTED // Session terminiert
    }
}