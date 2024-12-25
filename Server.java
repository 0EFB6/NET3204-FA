import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {
    public static void main(String[] args) {
        try {
            Impl obj = new Impl();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("FacebookService", obj);
            System.out.println("Facebook Server is ready. Please launch Client.java to use Facebook");
        } catch (RemoteException e) {
            System.err.println("Server exception: " + e.toString());
        }
    }
}