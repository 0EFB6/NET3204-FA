import java.rmi.Remote;
import java.rmi.RemoteException;

interface ClientCallback extends Remote {
    void notifyNewMessage(String from, String message) throws RemoteException;
    void notifyNewPost(String from, String content) throws RemoteException;
    void notifyNewNotification(String notification) throws RemoteException;
}