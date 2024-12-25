import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class DatabaseManager {
    private static final String DATA_DIR = "facebook_data/";
    
    static {
        new File(DATA_DIR).mkdirs();
    }

    private static class TimestampedNotification {
        long timestamp;
        String notification;
        TimestampedNotification(long timestamp, String notification) {
            this.timestamp = timestamp;
            this.notification = notification;
        }
    }
    
    static boolean saveUser(String username, String password) {
        try {
            Files.write(Paths.get(DATA_DIR + "users.txt"), (username + ":" + password + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            return true;
        }
        catch (IOException e) {}
        return false;
    }

    static boolean userExists(String username) {
        try {return Files.lines(Paths.get(DATA_DIR + "users.txt")).anyMatch(line -> line.split(":")[0].equals(username));}
        catch (IOException e) {return false;}
    }
    
    static boolean verifyUser(String username, String password) {
        try {return Files.lines(Paths.get(DATA_DIR + "users.txt")).anyMatch(line -> line.equals(username + ":" + password));}
        catch (IOException e) {return false;}
    }

    static void saveFriend(String username, String friend) {
        try {Files.write(Paths.get(DATA_DIR + username + "_friends.txt"), (friend + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);}
        catch (IOException e) {}
    }

    static Set<String> loadFriends(String username) {
        Set<String> friends = new HashSet<>();
        try {
            Path path = Paths.get(DATA_DIR + username + "_friends.txt");
            if (Files.exists(path)) Files.lines(path).forEach(friends::add);
        }
        catch (IOException e) {}
        return friends;
    }
    
    static void savePost(String username, Post post) {
        try {
            String postData = post.id + ":" + post.timestamp + ":" + post.content + "\n";
            Files.write(Paths.get(DATA_DIR + username + "_posts.txt"), postData.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e) {}
    }
    
    static List<Post> loadPosts(String username) {
        List<Post> posts = new ArrayList<>();
        try {
            Path path = Paths.get(DATA_DIR + username + "_posts.txt");
            if (Files.exists(path)) {
                Files.lines(path).forEach(line -> {
                    String[] parts = line.split(":", 3);
                    if (parts.length == 3) {
                        Post post = new Post(Integer.parseInt(parts[0]), username, parts[2]);
                        post.timestamp = Long.parseLong(parts[1]);
                        posts.add(post);
                    }
                });
            }
        }
        catch (IOException e) {}
        return posts;
    }

    static void saveConversation(String sender, String receiver, String content) {
        try {
            String message = (System.currentTimeMillis() + (8 * 60 * 60 * 1000)) + ":" + sender + ":" + content + "\n";
            String conversationFile = getConversationFileName(sender, receiver);            
            Files.write(Paths.get(DATA_DIR + conversationFile), message.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e) {}
    }

    static List<Message> loadConversation(String user1, String user2) {
        List<Message> messages = new ArrayList<>();
        try {
            String conversationFile = getConversationFileName(user1, user2);
            Path path = Paths.get(DATA_DIR + conversationFile);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    String[] parts = line.split(":", 3);
                    if (parts.length == 3) {
                        long timestamp = Long.parseLong(parts[0]);
                        String sender = parts[1];
                        String content = parts[2];
                        messages.add(new Message(sender, sender.equals(user1) ? user2 : user1, content, timestamp));
                    }
                }
                messages.sort((m1, m2) -> Long.compare(m1.timestamp, m2.timestamp));
            }
        }
        catch (IOException e) {}
        return messages;
    }

    static void updatePost(String username, Post post) {
        Path path = Paths.get(DATA_DIR + username + "_posts.txt");
        List<String> updatedPosts = new ArrayList<>();
    
        try {
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    String[] parts = line.split(":", 2); 
                    if (parts.length < 2) continue;
                    int postId = Integer.parseInt(parts[0]);
                    if (postId == post.id) {
                        String updatedPost = post.id + ":" + post.timestamp + ":" + post.content + "\n";
                        updatedPosts.add(updatedPost);
                    }
                    else updatedPosts.add(line + "\n");
                }    
                Files.write(path, updatedPosts, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
        catch (IOException e) {}
    }

    static void saveFriendRequest(String from, String to) {
        try {
            String requestFile = DATA_DIR + to + "_friendRequests.txt";
            if (Files.exists(Paths.get(requestFile))) {
                List<String> requests = Files.readAllLines(Paths.get(requestFile));
                if (requests.contains(from)) return;
            }
            Files.write(Paths.get(requestFile), (from + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e) {}
    }

    static List<String> loadFriendRequests(String username) {
        List<String> requests = new ArrayList<>();
        try {
            Path path = Paths.get(DATA_DIR + username + "_friendRequests.txt");
            if (Files.exists(path)) requests = Files.readAllLines(path);
        }
        catch (IOException e) {}
        return requests;
    }
        
    static void removeFriendRequest(String to, String from) {
        try {
            String requestFile = DATA_DIR + to + "_friendRequests.txt";
            if (Files.exists(Paths.get(requestFile))) {
                List<String> requests = Files.readAllLines(Paths.get(requestFile));
                requests.remove(from);
                Files.write(Paths.get(requestFile), requests, StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
        catch (IOException e) {}
    }
    
    static void saveNotification(String username, String notification) {
        try {
            String notificationFile = DATA_DIR + username + "_notifications.txt";
            Files.write(Paths.get(notificationFile), ((System.currentTimeMillis() + (8 * 60 * 60 * 1000)) + ":" + notification + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e) {}
    }

    static List<String> loadNotifications(String username) {
        List<String> notifications = new ArrayList<>();
        try {
            Path path = Paths.get(DATA_DIR + username + "_notifications.txt");
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                List<TimestampedNotification> timestampedNotifications = new ArrayList<>();                
                for (String line : lines) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        long timestamp = Long.parseLong(parts[0]);
                        String notification = parts[1];
                        timestampedNotifications.add(new TimestampedNotification(timestamp, notification));
                    }
                }
                timestampedNotifications.sort((n1, n2) -> Long.compare(n2.timestamp, n1.timestamp));
                for (TimestampedNotification tn : timestampedNotifications) {
                    java.util.Date date = new java.util.Date(tn.timestamp);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    notifications.add("[" + sdf.format(date) + "] " + tn.notification);
                }
            }
        }
        catch (IOException e) {}
        return notifications;
    }
    
    static boolean deletePost(String username, int postId) {
        Path path = Paths.get(DATA_DIR + username + "_posts.txt");
        try {
            if (Files.exists(path)) {
                List<String> posts = Files.readAllLines(path);
                List<String> updatedPosts = posts.stream().filter(line -> !line.startsWith(postId + ":")).collect(Collectors.toList());
                Files.write(path, updatedPosts, StandardOpenOption.TRUNCATE_EXISTING);
                return true;
            }
        }
        catch (IOException e) {}
        return false;
    }
    
    private static String getConversationFileName(String user1, String user2) {
        return (user1.compareTo(user2) < 0) ? user1 + "_" + user2 + "_conversation.txt" : user2 + "_" + user1 + "_conversation.txt";
    }
}