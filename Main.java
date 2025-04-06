
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class Main {
    static final String DATASET_PATH = "/Users/mannnandwal/Desktop/SOCOFing/Real";
    static final String DB_URL = "jdbc:mysql://localhost:3306/biometric_voting";
    static final String USER = "root"; // change as needed
    static final String PASS = "Fuck98266"; // change as needed

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- Welcome to Biometric Voting System ---");
            System.out.println("1. Log In (Using Fingerprint)");
            System.out.println("2. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    login(scanner);
                    break;
                case 2:
                    System.out.println("Exiting system...");
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static void login(Scanner scanner) {
        System.out.print("Enter User ID (e.g., 001 or 4[ADMIN]): ");
        String userId = scanner.nextLine();

        System.out.print("Enter path to fingerprint image: ");
        String fingerprintPath = scanner.nextLine();

        File inputFile = new File(fingerprintPath);

        try {
            if (matchFingerprint(inputFile, userId)) {
                System.out.println("Fingerprint matched.");
                if (userId.startsWith("4")) {
                    showAdminMenu();
                } else {
                    showVoterMenu(userId);
                }
            } else {
                System.out.println("Fingerprint mismatch or user not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean matchFingerprint(File inputFile, String userId) throws Exception {
        File datasetDir = new File(DATASET_PATH);
        File[] userSamples = datasetDir.listFiles(f -> f.getName().startsWith(userId + "__") ||
                f.getName().startsWith(String.format("%03d", Integer.parseInt(userId)) + "__"));

        if (userSamples == null || userSamples.length == 0) {
            System.out.println("No fingerprint samples found for user: " + userId);
            return false;
        }

        BufferedImage inputImg = ImageIO.read(inputFile);
        for (File sample : userSamples) {
            BufferedImage storedImg = ImageIO.read(sample);
            if (compareImages(inputImg, storedImg)) {
                return true;
            }
        }
        return false;
    }


    private static boolean compareImages(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) return false;

        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) return false;
            }
        }
        return true;
    }

    private static void showAdminMenu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. View Voters List");
            System.out.println("2. Add Candidate");
            System.out.println("3. View Voter Turnout");
            System.out.println("4. Count Votes");
            System.out.println("5. Clear Voter Turnout");
            System.out.println("6. Logout");

            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    viewVoters();
                    break;
                case 2:
                    addCandidate(scanner);
                    break;
                case 3:
                    viewVoterTurnout();
                    break;
                case 4:
                    countVotes();
                    break;
                case 5:
                    clearVoterTurnout();
                    break;
                case 6:
                    System.out.println("Logging out...");
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static void viewVoters() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT user_id FROM users WHERE role='voter'");
            System.out.println("\n--- Voter List ---");
            while (rs.next()) {
                System.out.println(rs.getString("user_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addCandidate(Scanner scanner) {
        System.out.print("Enter candidate name: ");
        String name = scanner.nextLine();
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "INSERT INTO candidates (name) VALUES (?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.executeUpdate();
            System.out.println("Candidate added.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void viewVoterTurnout() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT voter_id FROM voter_votes");
            System.out.println("\n--- Voters Who Voted ---");
            while (rs.next()) {
                System.out.println(rs.getString("voter_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void showVoterMenu(String userId) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- Voter Menu (" + userId + ") ---");
            System.out.println("1. Vote");
            System.out.println("2. Logout");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    castVote(userId, scanner);
                    break;
                case 2:
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }


    private static void castVote(String userId, Scanner scanner) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String checkSql = "SELECT * FROM voter_votes WHERE voter_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, userId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                System.out.println("You have already voted.");
                return;
            }

            Statement stmt = conn.createStatement();
            ResultSet candidates = stmt.executeQuery("SELECT * FROM candidates");
            System.out.println("\n--- Candidates ---");
            while (candidates.next()) {
                System.out.println(candidates.getInt("id") + ". " + candidates.getString("name"));
            }

            System.out.print("Enter candidate ID to vote: ");
            int voteFor = scanner.nextInt();
            scanner.nextLine();

            // Encrypt vote
            String encryptedVote = RSAUtil.encrypt(String.valueOf(voteFor));

            // Store encrypted vote
            String voteSql = "INSERT INTO voter_votes (voter_id, encrypted_vote) VALUES (?, ?)";
            PreparedStatement voteStmt = conn.prepareStatement(voteSql);
            voteStmt.setString(1, userId);
            voteStmt.setString(2, encryptedVote);
            voteStmt.executeUpdate();
            System.out.println("Encrypted vote recorded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void clearVoterTurnout() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM voter_votes");
            System.out.println("Voter turnout cleared. Ready for next election.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private static void countVotes() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            ResultSet voteRs = stmt.executeQuery("SELECT encrypted_vote FROM voter_votes");
            Map<Integer, Integer> voteCount = new HashMap<>();

            while (voteRs.next()) {
                String encrypted = voteRs.getString("encrypted_vote");
                String decrypted = RSAUtil.decrypt(encrypted);
                int candidateId = Integer.parseInt(decrypted);
                voteCount.put(candidateId, voteCount.getOrDefault(candidateId, 0) + 1);
            }

            ResultSet candidates = stmt.executeQuery("SELECT * FROM candidates");
            System.out.println("\n--- Vote Count ---");
            while (candidates.next()) {
                int id = candidates.getInt("id");
                String name = candidates.getString("name");
                int count = voteCount.getOrDefault(id, 0);
                System.out.println(name + ": " + count + " vote(s)");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
