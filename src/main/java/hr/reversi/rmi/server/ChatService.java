package hr.reversi.rmi.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatService extends Remote {

    String REMOTE_OBJECT_NAME = "hr.reversi.rmi.service";

    void sendMessage(String newMessage) throws RemoteException;

    List<String> getChatHistory() throws RemoteException;

}
