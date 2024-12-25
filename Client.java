import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class Client {
    private Service service;
    private ClientCallback callback;
    private String currentUser = null;
    private Scanner scanner = new Scanner(System.in);

    public Client() {
        try {
            Registry registry = LocateRegistry.getRegistry(null, 1099);
            service = (Service) registry.lookup("FacebookService");
            callback = new ClientCallbackImpl();
        }
        catch (NotBoundException | RemoteException e) {
        }
    }

    private void cls() throws RemoteException {
        try {
            if (System.getProperty("os.name").contains("Windows")) new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else new ProcessBuilder("clear").inheritIO().start().waitFor();
        }
        catch (IOException | InterruptedException e) {}
    }

    public void start() {
        while (true) {
            if (currentUser == null) 
                showLoginMenu();
            else 
                showMainMenu();
        }
    }

    private void sendFriendRequest() throws RemoteException {
        System.out.print("Enter username to send friend request: ");
        String friend = scanner.nextLine();
        cls();
        if (service.sendFriendRequest(currentUser, friend)) System.out.println("Friend request to " + friend + " sent successfully!");
        else System.out.println("Fail to send friend request - user '" + friend + "' does not exist!");
    }

    private void showLoginMenu() {
        System.out.println("\n=== Facebook ===");
        System.out.println("1. Login\n2. Register\n3. Exit");
        System.out.print("Choose option: ");
        try {
            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1 -> login();
                case 2 -> register();
                case 3 -> System.exit(0);
            }
        }
        catch (Exception e) {
            System.err.println("Error: Please enter integer ranging from 1 to 3 only!");
            scanner.nextLine();
        }
    }

    private void login() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine();
            System.out.print("Password: ");
            String password = scanner.nextLine();
            cls();
            if (service.login(username, password, callback)) {
                currentUser = username;
                System.out.println("Login successful!");
            }
            else System.out.println("Login failed!");
        }
        catch (RemoteException e) {
            System.err.println("Login error: " + e.getMessage());
        }
    }

    private void register() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine();
            System.out.print("Password: ");
            String password = scanner.nextLine();
            System.out.print("Email: ");
            String email = scanner.nextLine();
            System.out.print("Phone Number: ");
            String phone = scanner.nextLine();
            cls();
            if (service.register(username, password)) System.out.println("Registration successful! Your username will be used to communicate with friends in the application.");
            else System.out.println("Registration failed - username already exist!");
        }
        catch (RemoteException e) {
            System.err.println("Registration error: " + e.getMessage());
        }
    }

    private void showMainMenu() {
        try {
            System.out.println("===================================================");
            System.out.println("                 Welcome " + currentUser);
            System.out.println("    Feibook wishes you a Happy Merry Christmas!");
            System.out.println("===================================================");
            System.out.println("1. Create Post\n2. View News Feed\n3. Send Friend Request\n4. View Friend Requests (" + service.getPendingFriendRequests(currentUser).size() + ")\n5. View Friends\n6. Send Message\n7. View Messages\n8. View Notifications\n9. Manage Posts\n10. Logout");
            System.out.print("Choose option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1 -> createPost();
                case 2 -> viewNewsFeed();
                case 3 -> sendFriendRequest();
                case 4 -> viewFriendRequests();
                case 5 -> viewFriends();
                case 6 -> sendMessage();
                case 7 -> viewMessages();
                case 8 -> viewNotifications();
                case 9 -> managePosts();
                case 10 -> logout();
                default -> System.out.println("Invalid option!");
            }
        }
        catch (RemoteException e) {
            System.err.println("Error: Please enter integer ranging from 1 to 10 only!");
            scanner.nextLine();
        }
    }

    private void viewFriendRequests() throws RemoteException {
        cls();
        List<String> requests = service.getPendingFriendRequests(currentUser);
        if (requests.isEmpty()) {
            System.out.println("No pending friend requests!");
            return;
        }
        System.out.println("\n=== Friend Requests ===");
        for (String requester : requests) {
            System.out.println("From: " + requester);
            System.out.println("1. Accept\n2. Reject\n3. Skip");
            System.out.print("Choose option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1 -> {
                    if (service.acceptFriendRequest(currentUser, requester)) System.out.println("Friend request accepted!");
                }
                case 2 -> {
                    if (service.rejectFriendRequest(currentUser, requester)) System.out.println("Friend request rejected!");
                }
            }
        }
    }

    private void viewNotifications() throws RemoteException {
        cls();
        List<String> notifications = service.getNotifications(currentUser);
        if (notifications.isEmpty()) {
            cls();
            System.out.println("No notifications!");
            return;
        }
        System.out.println("\n=== Notifications ===");
        for (String notification : notifications) System.out.println(notification);
    }
    
    private void managePosts() throws RemoteException {
        cls();
        List<Post> userPosts = service.getUserPosts(currentUser);
        if (userPosts.isEmpty()) {
            System.out.println("You haven't created any posts yet!");
            return;
        }
        for (Post post : userPosts) {
            System.out.println("\n=== Post ===");
            System.out.println("ID: " + post.id + "\nContent: " + post.content + "\nLikes: " + post.likes.size() + "\nComments: " + post.comments.size());
            System.out.println("\n1. Edit\n2. Delete\n3. Next post\n4. Return to menu");
            System.out.print("Choose option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1 -> {
                    System.out.print("Enter new content: ");
                    String newContent = scanner.nextLine();
                    cls();
                    if (service.editPost(currentUser, post.id, newContent)) System.out.println("Post updated successfully!");
                    else System.out.println("Failed to update post!");
                }
                case 2 -> {
                    cls();
                    if (service.deletePost(currentUser, post.id)) System.out.println("Post deleted successfully!");
                    else System.out.println("Failed to delete post!");
                }
                case 3 -> {
                    cls();
                    continue;
                }
                case 4 -> {
                    cls();
                    return;
                }
            }
        }
    }

    private void createPost() throws RemoteException {
        cls();
        System.out.print("Enter post content: ");
        String content = scanner.nextLine();
        int postId = service.createPost(currentUser, content);
        System.out.println("Post created with ID: " + postId);
    }

    private void viewNewsFeed() throws RemoteException {
        cls();
        List<Post> newsFeed = service.getNewsFeed(currentUser);
        if (newsFeed.isEmpty()) {
            System.out.println("No posts in news feed!");
            return;
        }
        for (Post post : newsFeed) {
            System.out.println("\n=== Post ===" + "\nFrom: " + post.username + "\nContent: " + post.content + "\nLikes: " + post.likes.size() + "\nComments: " + post.comments.size());
            System.out.println("\n=== Comments ===");
            if (post.comments.isEmpty()) System.out.println("No comments yet");
            else for (Comment comment : post.comments) System.out.println(comment.username + ": " + comment.content);
            System.out.println("\n=== Options ===\n1. Like\n2. Comment\n3. Next post\n4. Return to menu");
            System.out.print("Choose option: ");
            int action = scanner.nextInt();
            scanner.nextLine();
            switch (action) {
                case 1 -> {
                    service.likePost(currentUser, post.id);
                    cls();
                    System.out.println("Post liked!");
                }
                case 2 -> {
                    System.out.print("Enter comment: ");
                    String comment = scanner.nextLine();
                    service.commentPost(currentUser, post.id, comment);
                    cls();
                    System.out.println("Comment added!");
                }
                case 3 -> {
                    cls();
                    continue;
                }
                case 4 -> {
                    cls();
                    return;
                }
            }
        }
        cls();
    }

    private void viewFriends() throws RemoteException {
        cls();
        List<String> friends = service.getFriendList(currentUser);
        if (friends.isEmpty()) {
            System.out.println("No friends yet!");
            return;
        }
        System.out.println("=== Friends ===");
        for (String friend : friends) System.out.println("- " + friend);
        System.out.println("\n");
    }

    private void sendMessage() throws RemoteException {
        cls();
        System.out.print("Enter recipient's username: ");
        String to = scanner.nextLine();
        System.out.print("Enter message: ");
        String message = scanner.nextLine();
        cls();
        if (service.sendMessage(currentUser, to, message)) System.out.println("Message sent successfully!");
        else System.out.println("Failed to send message - user does not exist!");
    }

    private void viewMessages() throws RemoteException {
        cls();
        List<String> friends = service.getFriendList(currentUser);
        if (friends.isEmpty()) {
            System.out.println("You have no friends to chat with! So sad :(");
            return;
        }
        while (true) {
            cls();
            System.out.println("\n=== Select friend to view conversation ===");
            for (int i = 0; i < friends.size(); i++)
                System.out.println((i + 1) + ". " + friends.get(i));
            System.out.println((friends.size() + 1) + ". Return to menu\nChoose friend:");            
            try {
                int choice = scanner.nextInt();
                scanner.nextLine();
                cls();
                if (choice == friends.size() + 1) {cls(); return;}
                if (choice > friends.size()) {
                    System.out.println("Invalid choice! Please try again!");
                    continue;
                }
                String friend = friends.get(choice - 1);
                displayConversation(friend);
            }
            catch (RemoteException e) {
                System.err.println("Error: Please enter integer ranging from 1 to " + (friends.size() + 1) + " only!");
                scanner.nextLine();
            }
        }
    }

    private void displayConversation(String friend) throws RemoteException {
        while (true) {
            List<Message> messages = service.getMessages(currentUser, friend);
            System.out.println("\n=== Conversation with " + friend + " ===");
            if (messages.isEmpty()) System.out.println("No messages yet!");
            else {
                for (Message msg : messages) {
                    String prefix = msg.from.equals(currentUser) ? "You" : friend;
                    System.out.println(prefix + ": " + msg.content);
                }
            }
            System.out.println("\n1. Send message\n2. Return to friend list\nChoose option:");
            int choice = scanner.nextInt();
            scanner.nextLine();
            cls();          
            if (choice == 1) {
                System.out.print("Enter message: ");
                String message = scanner.nextLine();
                if (service.sendMessage(currentUser, friend, message)) System.out.println("Message sent successfully!");
                else System.out.println("Failed to send message!");
            }
            else return;
        }
    }

    private void logout() throws RemoteException {
        service.logout(currentUser);
        currentUser = null;
        cls();
        System.out.println("Logged out successfully!");
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}