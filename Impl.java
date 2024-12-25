import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Impl extends UnicastRemoteObject implements Service {
    @SuppressWarnings("FieldMayBeFinal")
    private Map<String, ClientCallback> activeClients = new ConcurrentHashMap<>();
    @SuppressWarnings("FieldMayBeFinal")
    private Map<String, Set<String>> friends;
    @SuppressWarnings("FieldMayBeFinal")
    private Map<String, List<Post>> posts;
    @SuppressWarnings("FieldMayBeFinal")
    private Map<String, List<String>> notifications;
    public static int nextPostId = 5;
    @SuppressWarnings("FieldMayBeFinal")
    private Map<String, List<String>> pendingFriendRequests;
    
    public Impl() throws RemoteException {
        super();
        friends = new ConcurrentHashMap<>();
        posts = new ConcurrentHashMap<>();
        notifications = new ConcurrentHashMap<>();
        pendingFriendRequests = new ConcurrentHashMap<>();
        loadAllUserPosts();
    }

    private void loadAllUserPosts() {
        try {
            Path usersPath = Paths.get("facebook_data/users.txt");
            if (Files.exists(usersPath)) {
                List<String> users = Files.lines(usersPath).map(line -> line.split(":")[0]).collect(Collectors.toList());                
                for (String username : users) {posts.put(username, new ArrayList<>(DatabaseManager.loadPosts(username)));}
            }
        }
        catch (IOException e) {System.err.println("Error loading user posts: " + e.getMessage());}
    }
    
    @Override
    public boolean register(String username, String password) throws RemoteException {
        System.out.println("[LOG] Processing register request for user " + username);
        if (DatabaseManager.userExists(username)) return false;
        boolean success = DatabaseManager.saveUser(username, password);
        if (success) {
            friends.put(username, new HashSet<>());
            posts.put(username, new ArrayList<>());
            notifications.put(username, new ArrayList<>());
        }
        return success;
    }
    
    @Override
    public boolean login(String username, String password, ClientCallback callback) throws RemoteException {
        System.out.println("[LOG] Processing login request for user " + username);
        if (DatabaseManager.verifyUser(username, password)) {
            activeClients.put(username, callback);
            friends.put(username, new HashSet<>(DatabaseManager.loadFriends(username)));
            if (!posts.containsKey(username)) posts.put(username, new ArrayList<>(DatabaseManager.loadPosts(username)));
            notifications.put(username, new ArrayList<>(DatabaseManager.loadNotifications(username)));
            pendingFriendRequests.put(username, new ArrayList<>(DatabaseManager.loadFriendRequests(username)));
            return true;
        }
        return false;
    }

    @Override
    public boolean sendMessage(String from, String to, String content) throws RemoteException {
        System.out.println("[LOG] Processing sendMessage request for user " + from + " to " + to);
        if (!DatabaseManager.userExists(to)) return false;
        DatabaseManager.saveConversation(from, to, content);
        DatabaseManager.saveNotification(to, "New message from " + from + ": " + (content.length() > 50 ? content.substring(0, 47) + "..." : content));
        ClientCallback recipient = activeClients.get(to);
        if (recipient != null) recipient.notifyNewMessage(from, content);
        return true;
    }

    @Override
    public boolean deletePost(String username, int postId) throws RemoteException {
        System.out.println("[LOG] Processing deletePost request for user " + username);
        List<Post> userPosts = posts.get(username);
        if (userPosts != null) {
            boolean removed = userPosts.removeIf(post -> post.id == postId);
            if (removed) {
                DatabaseManager.deletePost(username, postId);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean editPost(String username, int postId, String newContent) throws RemoteException {
        System.out.println("[LOG] Processing editPost request for user " + username);
        List<Post> userPosts = posts.get(username);
        if (userPosts != null) {
            for (Post post : userPosts) {
                if (post.id == postId) {
                    post.content = newContent;
                    DatabaseManager.updatePost(username, post);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean sendFriendRequest(String from, String to) throws RemoteException {
        System.out.println("[LOG] Processing sendFriendRequest request for user " + from + " to " + to);
        if (!DatabaseManager.userExists(to)) return false;
        DatabaseManager.saveFriendRequest(from, to);
        DatabaseManager.saveNotification(to, from + " sent you a friend request");
        ClientCallback recipientCallback = activeClients.get(to);
        if (recipientCallback != null) recipientCallback.notifyNewNotification(from + " sent you a friend request");
        return true;
    }

    @Override
    public boolean acceptFriendRequest(String username, String requester) throws RemoteException {
        System.out.println("[LOG] Processing acceptFriendRequest request for user " + username);
        List<String> requests = DatabaseManager.loadFriendRequests(username);
        if (requests.contains(requester)) {
            friends.get(username).add(requester);
            friends.get(requester).add(username);
            DatabaseManager.saveFriend(username, requester);
            DatabaseManager.saveFriend(requester, username);
            DatabaseManager.removeFriendRequest(username, requester);
            DatabaseManager.saveNotification(requester, username + " accepted your friend request");
            ClientCallback requesterCallback = activeClients.get(requester);
            if (requesterCallback != null) requesterCallback.notifyNewNotification(username + " accepted your friend request");
            return true;
        }
        return false;
    }
    
    @Override
    public boolean rejectFriendRequest(String username, String requester) throws RemoteException {
        System.out.println("[LOG] Processing rejectFriendRequest request for user " + username);
        DatabaseManager.removeFriendRequest(username, requester);
        return true;
    }

    @Override
    public List<String> getFriendList(String username) throws RemoteException {
        System.out.println("[LOG] Processing getFriendList request for user " + username);
        return new ArrayList<>(friends.get(username));
    }

    @Override
    public List<Post> getNewsFeed(String username) throws RemoteException {
        try {
            System.out.println("[LOG] Processing getNewsFeed request for user " + username);
            List<Post> newsFeed = new ArrayList<>();
            Set<String> userFriends = friends.get(username);
            for (String friend : userFriends) newsFeed.addAll(posts.get(friend));
            newsFeed.sort((p1, p2) -> p2.id - p1.id);
            
            return newsFeed;
        }
        catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<Message> getMessages(String username, String friend) throws RemoteException {
        System.out.println("[LOG] Processing getMessages request for user " + username + " from " + friend);
        return DatabaseManager.loadConversation(username, friend);
    }

    @Override
    public List<String> getNotifications(String username) throws RemoteException {
        System.out.println("[LOG] Processing getNotifications request for user " + username);
        return DatabaseManager.loadNotifications(username);
    }
    
    @Override
    public List<String> getPendingFriendRequests(String username) throws RemoteException {
        System.out.println("[LOG] Processing getPendingFriendRequests request for user " + username);
        return DatabaseManager.loadFriendRequests(username);
    }

    @Override
    public List<Post> getUserPosts(String username) throws RemoteException {
        System.out.println("[LOG] Processing getUserPosts request for user " + username);
        return posts.getOrDefault(username, new ArrayList<>());
    }

    @Override
    public int createPost(String username, String content) throws RemoteException {
        System.out.println("[LOG] Processing createPost request for user " + username);
        Post post = new Post(nextPostId++, username, content);
        posts.computeIfAbsent(username, k -> new ArrayList<>()).add(post);
        DatabaseManager.savePost(username, post);
        try {
            for (String friend : friends.get(username)) {
                DatabaseManager.saveNotification(friend, username + " created a new post: " + (content.length() > 50 ? content.substring(0, 47) + "..." : content));
                ClientCallback friendCallback = activeClients.get(friend);
                if (friendCallback != null) friendCallback.notifyNewPost(username, content);
            }
        }
        catch (RemoteException e) {}
        return post.id;
    }

    @Override
    public void logout(String username) throws RemoteException {
        System.out.println("[LOG] Logging out user " + username);
        activeClients.remove(username);
    }

    @Override
    public void likePost(String username, int postId) throws RemoteException {
        System.out.println("[LOG] Processing likePost request for user " + username);
        for (List<Post> userPosts : posts.values()) {
            for (Post post : userPosts) {
                if (post.id == postId) {
                    if (post.likes.contains(username)) return;
                    post.likes.add(username);
                    DatabaseManager.updatePost(post.username, post);
                    DatabaseManager.saveNotification(post.username, username + " liked your post");
                    ClientCallback ownerCallback = activeClients.get(post.username);
                    if (ownerCallback != null) ownerCallback.notifyNewNotification(username + " liked your post");
                    return;
                }
            }
        }
    }
    
    @Override
    public void commentPost(String username, int postId, String comment) throws RemoteException {
        System.out.println("[LOG] Processing commentPost request for user " + username);
        if (comment == null || comment.trim().isEmpty()) {
            System.out.println("Error: Comment cannot be empty.");
            return;
        }
        for (List<Post> userPosts : posts.values()) {
            for (Post post : userPosts) {
                if (post.id == postId) {
                    post.comments.add(new Comment(username, comment));
                    DatabaseManager.updatePost(post.username, post);
                    DatabaseManager.saveNotification(post.username, username + " commented on your post");
                    ClientCallback ownerCallback = activeClients.get(post.username);
                    if (ownerCallback != null) ownerCallback.notifyNewNotification(username + " commented on your post");
                    return;
                }
            }
        }
    }
}