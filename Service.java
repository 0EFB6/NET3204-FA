import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

interface Service extends Remote {
    boolean register(String username, String password) throws RemoteException;
    boolean login(String username, String password, ClientCallback callback) throws RemoteException;
    boolean sendMessage(String from, String to, String content) throws RemoteException;
    boolean deletePost(String username, int postId) throws RemoteException;
    boolean editPost(String username, int postId, String newContent) throws RemoteException;
    boolean sendFriendRequest(String from, String to) throws RemoteException;
    boolean acceptFriendRequest(String username, String requester) throws RemoteException;
    boolean rejectFriendRequest(String username, String requester) throws RemoteException;
    
    List<String> getFriendList(String username) throws RemoteException;
    List<Post> getNewsFeed(String username) throws RemoteException;
    List<Message> getMessages(String username, String friend) throws RemoteException;
    List<String> getNotifications(String username) throws RemoteException;
    List<String> getPendingFriendRequests(String username) throws RemoteException;
    List<Post> getUserPosts(String username) throws RemoteException;
    
    int createPost(String username, String content) throws RemoteException;

    void logout(String username) throws RemoteException;
    void likePost(String username, int postId) throws RemoteException;
    void commentPost(String username, int postId, String comment) throws RemoteException;
}

class Post implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    int id;
    String username;
    String content;
    List<String> likes = new ArrayList<>();
    List<Comment> comments = new ArrayList<>();
    long timestamp;
    
    Post(int id, String username, String content) {
        this.id = id;
        this.username = username;
        this.content = content;
        this.timestamp = System.currentTimeMillis() + (8 * 60 * 60 * 1000);
    }
}

class Comment implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    String username;
    String content;
    
    Comment(String username, String content) {
        this.username = username;
        this.content = content;
    }
}

class Message implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    String from;
    @SuppressWarnings("unused")
    String to;
    String content;
    long timestamp;
    
    Message(String from, String to, String content, long timestamp) {
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = timestamp;
    }
}