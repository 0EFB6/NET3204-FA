import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
    private static final long serialVersionUID = 1L;
    
    public ClientCallbackImpl() throws RemoteException {
        super();
    }
    
    @Override
    public void notifyNewMessage(String from, String message) throws RemoteException {
        System.out.println("\n\n[NEW MESSAGE from " + from + "]: " + message);
        System.out.print("\nEnter choice: ");
    }
    
    @Override
    public void notifyNewPost(String from, String content) throws RemoteException {
        System.out.println("\n\n[NEW POST from " + from + "]: " + content);
        System.out.print("\nEnter choice: ");
    }
    
    @Override
    public void notifyNewNotification(String notification) throws RemoteException {
        System.out.println("\n\n[NOTIFICATION]: " + notification);
        System.out.print("\nEnter choice: ");
    }
}