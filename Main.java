import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * CineBook - standalone Java Swing movie-ticket booking demo.
 * Requires only a JDK (no external libraries, Maven, Gradle, or SQLite driver).
 * Data is stored locally in data/cinebook-data.txt.
 */
public class Main extends JFrame {
    private static final String DATA_DIR = "data";
    private static final String DATA_FILE = DATA_DIR + File.separator + "cinebook-data.txt";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final List<Movie> movies = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final DefaultListModel<String> movieModel = new DefaultListModel<>();
    private final DefaultListModel<String> bookingModel = new DefaultListModel<>();
    private JList<String> movieList;
    private JList<String> bookingList;
    private JLabel poster;
    private JTextArea details;

    private static class Movie {
        String title, genre, language, showTime;
        int duration;
        double price;
        Movie(String title, String genre, String language, int duration, double price, String showTime) {
            this.title=title; this.genre=genre; this.language=language; this.duration=duration;
            this.price=price; this.showTime=showTime;
        }
    }
    private static class Booking {
        String id, movieTitle, seats, customer, bookedAt;
        double total;
        Booking(String id, String movieTitle, String seats, String customer, String bookedAt, double total) {
            this.id=id; this.movieTitle=movieTitle; this.seats=seats; this.customer=customer;
            this.bookedAt=bookedAt; this.total=total;
        }
    }

    public Main() {
        super("CineBook | Movie Ticket Booking");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(850, 560));
        setSize(1050, 680);
        setLocationRelativeTo(null);
        loadData();
        buildInterface();
        refreshMovies();
        refreshBookings();
        if (!movies.isEmpty()) movieList.setSelectedIndex(0);
    }

    private void buildInterface() {
        Color navy = new Color(22, 29, 49);
        Color accent = new Color(105, 72, 190);
        Color background = new Color(245, 246, 250);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(background);
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout(12, 4));
        header.setBackground(navy);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));
        JLabel brand = new JLabel("CINEBOOK");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 27));
        JLabel tagline = new JLabel("Find your movie. Choose your seats. Enjoy the show.");
        tagline.setForeground(new Color(220, 223, 238));
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 13));
        header.add(brand, BorderLayout.WEST);
        header.add(tagline, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("Movies & Booking", moviesTab(accent, background));
        tabs.addTab("My Bookings", bookingsTab());
        tabs.addTab("Admin", adminTab(accent, background));
        root.add(tabs, BorderLayout.CENTER);

        JLabel footer = new JLabel("  CineBook Student Project • Java Swing • Local demo storage");
        footer.setBorder(new EmptyBorder(7, 10, 7, 10));
        footer.setForeground(new Color(90, 95, 110));
        root.add(footer, BorderLayout.SOUTH);
    }

    private JPanel moviesTab(Color accent, Color background) {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        panel.setBackground(background);

        JPanel left = cardPanel();
        left.setPreferredSize(new Dimension(300, 400));
        JLabel heading = new JLabel("Now Showing");
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        movieList = new JList<>(movieModel);
        movieList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        movieList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        movieList.setFixedCellHeight(42);
        movieList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showSelectedMovie(); });
        left.add(heading, BorderLayout.NORTH);
        left.add(new JScrollPane(movieList), BorderLayout.CENTER);

        JPanel right = cardPanel();
        poster = new JLabel("CINEBOOK", SwingConstants.CENTER);
        poster.setOpaque(true);
        poster.setBackground(new Color(31, 39, 65));
        poster.setForeground(Color.WHITE);
        poster.setFont(new Font("SansSerif", Font.BOLD, 30));
        poster.setPreferredSize(new Dimension(250, 170));

        details = new JTextArea("Select a movie to view details.");
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(new Font("SansSerif", Font.PLAIN, 15));
        details.setBackground(Color.WHITE);
        details.setBorder(new EmptyBorder(12, 0, 12, 0));

        JButton bookButton = styledButton("Book Tickets", accent);
        bookButton.addActionListener(e -> bookSelectedMovie());
        JPanel detailArea = new JPanel(new BorderLayout(8, 8));
        detailArea.setBackground(Color.WHITE);
        detailArea.add(details, BorderLayout.CENTER);
        detailArea.add(bookButton, BorderLayout.SOUTH);
        right.add(poster, BorderLayout.NORTH);
        right.add(detailArea, BorderLayout.CENTER);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.CENTER);
        return panel;
    }

    private JPanel bookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        panel.setBackground(new Color(245, 246, 250));
        JLabel heading = new JLabel("Booking History");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        bookingList = new JList<>(bookingModel);
        bookingList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JButton cancel = styledButton("Cancel Selected Booking", new Color(180, 55, 70));
        cancel.addActionListener(e -> cancelSelectedBooking());
        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(bookingList), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(cancel);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel adminTab(Color accent, Color background) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(background);
        panel.setBorder(new EmptyBorder(22, 30, 22, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 7, 7, 7);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        JTextField title = new JTextField();
        JTextField genre = new JTextField();
        JTextField language = new JTextField();
        JTextField duration = new JTextField("120");
        JTextField price = new JTextField("150");
        JTextField showTime = new JTextField("6:30 PM");
        addFormRow(panel, c, 0, "Movie title", title);
        addFormRow(panel, c, 1, "Genre", genre);
        addFormRow(panel, c, 2, "Language", language);
        addFormRow(panel, c, 3, "Duration (minutes)", duration);
        addFormRow(panel, c, 4, "Ticket price (₹)", price);
        addFormRow(panel, c, 5, "Show time", showTime);

        JButton add = styledButton("Add Movie", accent);
        c.gridx = 1; c.gridy = 6; c.gridwidth = 1;
        panel.add(add, c);
        JLabel note = new JLabel("<html><b>Demo admin panel</b><br>Added movies are saved on this computer. This is a local student demo; it does not process real payments.</html>");
        note.setForeground(new Color(80, 85, 100));
        c.gridx = 0; c.gridy = 7; c.gridwidth = 2;
        panel.add(note, c);

        add.addActionListener(e -> {
            try {
                String t = title.getText().trim();
                String g = genre.getText().trim();
                String l = language.getText().trim();
                String st = showTime.getText().trim();
                int d = Integer.parseInt(duration.getText().trim());
                double p = Double.parseDouble(price.getText().trim());
                if (t.isEmpty() || g.isEmpty() || l.isEmpty() || st.isEmpty() || d <= 0 || p <= 0) {
                    throw new IllegalArgumentException();
                }
                movies.add(new Movie(t, g, l, d, p, st));
                saveData();
                refreshMovies();
                title.setText(""); genre.setText(""); language.setText("");
                JOptionPane.showMessageDialog(this, "Movie added successfully.", "CineBook", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Enter valid details. Duration and price must be positive numbers.",
                        "Invalid input", JOptionPane.WARNING_MESSAGE);
            }
        });
        return panel;
    }

    private JPanel cardPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 228, 236)),
                new EmptyBorder(12, 12, 12, 12)));
        return p;
    }

    private void addFormRow(JPanel panel, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridy = row; c.gridx = 0; c.gridwidth = 1; c.weightx = 0.35;
        panel.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 0.65;
        panel.add(field, c);
    }

    private JButton styledButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        return b;
    }

    private void refreshMovies() {
        movieModel.clear();
        for (Movie m : movies) movieModel.addElement(m.title + "  •  " + m.genre);
    }

    private void showSelectedMovie() {
        int i = movieList.getSelectedIndex();
        if (i < 0 || i >= movies.size()) {
            details.setText("Select a movie to view details.");
            poster.setText("CINEBOOK");
            return;
        }
        Movie m = movies.get(i);
        poster.setText("<html><div style='text-align:center'>" + html(m.title) + "<br><span style='font-size:14px'>NOW SHOWING</span></div></html>");
        details.setText("MOVIE: " + m.title + "\n\nGenre: " + m.genre + "\nLanguage: " + m.language
                + "\nDuration: " + m.duration + " minutes\nShow time: " + m.showTime
                + "\nTicket price: ₹" + String.format(Locale.US, "%.2f", m.price)
                + "\n\nSelect “Book Tickets” to choose seats and confirm your booking.");
    }

    private String html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void bookSelectedMovie() {
        int index = movieList.getSelectedIndex();
        if (index < 0 || index >= movies.size()) {
            JOptionPane.showMessageDialog(this, "Please select a movie first.", "Select movie", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Movie m = movies.get(index);
        JTextField customer = new JTextField();
        String[] rows = {"A", "B", "C", "D", "E"};
        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.add(new JLabel("Customer name:"));
        form.add(customer);
        form.add(new JLabel("Choose seats (each seat costs ₹" + String.format(Locale.US, "%.2f", m.price) + "):"));
        JPanel seatGrid = new JPanel(new GridLayout(rows.length, 6, 5, 5));
        List<JToggleButton> seatButtons = new ArrayList<>();
        for (String row : rows) {
            for (int col = 1; col <= 6; col++) {
                JToggleButton seat = new JToggleButton(row + col);
                seat.setFocusPainted(false);
                seatGrid.add(seat);
                seatButtons.add(seat);
            }
        }
        form.add(seatGrid);
        int result = JOptionPane.showConfirmDialog(this, form, "Book " + m.title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        String name = customer.getText().trim();
        List<String> selected = new ArrayList<>();
        for (JToggleButton b : seatButtons) if (b.isSelected()) selected.add(b.getText());
        if (name.isEmpty() || selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter your name and select at least one seat.", "Missing details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Set<String> occupied = occupiedSeats(m.title);
        for (String seat : selected) {
            if (occupied.contains(seat)) {
                JOptionPane.showMessageDialog(this, "Seat " + seat + " is already booked. Please choose another seat.",
                        "Seat unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        String id = "CB" + (10000 + new Random().nextInt(90000));
        String seatText = String.join(", ", selected);
        double total = selected.size() * m.price;
        bookings.add(new Booking(id, m.title, seatText, name, TIME_FORMAT.format(LocalDateTime.now()), total));
        saveData();
        refreshBookings();
        JOptionPane.showMessageDialog(this,
                "Booking confirmed!\n\nBooking ID: " + id + "\nMovie: " + m.title + "\nSeats: " + seatText
                        + "\nTotal: ₹" + String.format(Locale.US, "%.2f", total),
                "CineBook - Confirmation", JOptionPane.INFORMATION_MESSAGE);
    }

    private Set<String> occupiedSeats(String movieTitle) {
        Set<String> occupied = new HashSet<>();
        for (Booking b : bookings) if (b.movieTitle.equals(movieTitle)) {
            occupied.addAll(Arrays.asList(b.seats.split(",\\s*")));
        }
        return occupied;
    }

    private void cancelSelectedBooking() {
        int i = bookingList.getSelectedIndex();
        if (i < 0 || i >= bookings.size()) {
            JOptionPane.showMessageDialog(this, "Select a booking to cancel.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Booking b = bookings.get(i);
        int answer = JOptionPane.showConfirmDialog(this, "Cancel booking " + b.id + " for " + b.movieTitle + "?",
                "Confirm cancellation", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            bookings.remove(i);
            saveData();
            refreshBookings();
            JOptionPane.showMessageDialog(this, "Booking cancelled.");
        }
    }

    private void refreshBookings() {
        bookingModel.clear();
        for (Booking b : bookings) {
            bookingModel.addElement(b.id + " | " + b.movieTitle + " | Seats: " + b.seats
                    + " | Customer: " + b.customer + " | ₹" + String.format(Locale.US, "%.2f", b.total));
        }
    }

    private java.sql.Connection connectDatabase() throws java.sql.SQLException {
        try { Files.createDirectories(Paths.get("data")); }
        catch (IOException ex) { throw new java.sql.SQLException("Cannot create data folder", ex); }
        return java.sql.DriverManager.getConnection("jdbc:sqlite:data/cinebook.db");
    }

    private void createTables(java.sql.Connection conn) throws java.sql.SQLException {
        try (java.sql.Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS movies (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, genre TEXT NOT NULL, language TEXT NOT NULL, duration INTEGER NOT NULL, price REAL NOT NULL, show_time TEXT NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS bookings (id TEXT PRIMARY KEY, movie_title TEXT NOT NULL, seats TEXT NOT NULL, customer TEXT NOT NULL, booked_at TEXT NOT NULL, total REAL NOT NULL)");
        }
    }

    private void loadData() {
        try (java.sql.Connection conn = connectDatabase()) {
            createTables(conn);
            try (java.sql.Statement st = conn.createStatement(); java.sql.ResultSet rs = st.executeQuery("SELECT title, genre, language, duration, price, show_time FROM movies ORDER BY id")) {
                while (rs.next()) movies.add(new Movie(rs.getString("title"), rs.getString("genre"), rs.getString("language"), rs.getInt("duration"), rs.getDouble("price"), rs.getString("show_time")));
            }
            try (java.sql.Statement st = conn.createStatement(); java.sql.ResultSet rs = st.executeQuery("SELECT id, movie_title, seats, customer, booked_at, total FROM bookings ORDER BY rowid")) {
                while (rs.next()) bookings.add(new Booking(rs.getString("id"), rs.getString("movie_title"), rs.getString("seats"), rs.getString("customer"), rs.getString("booked_at"), rs.getDouble("total")));
            }
            if (movies.isEmpty()) {
                movies.add(new Movie("Vikram", "Action / Thriller", "Tamil", 174, 150, "10:30 AM"));
                movies.add(new Movie("Manjummel Boys", "Adventure / Drama", "Malayalam", 135, 180, "1:30 PM"));
                movies.add(new Movie("Premalu", "Romantic Comedy", "Malayalam", 156, 160, "4:30 PM"));
                movies.add(new Movie("Aavesham", "Action / Comedy", "Malayalam", 158, 170, "7:30 PM"));
                saveData();
            }
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(null, "Database error: " + ex.getMessage() + "\nCheck that sqlite-jdbc.jar is in the lib folder.", "CineBook database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveData() {
        try (java.sql.Connection conn = connectDatabase()) {
            createTables(conn);
            conn.setAutoCommit(false);
            try (java.sql.Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM movies");
                st.executeUpdate("DELETE FROM bookings");
            }
            try (java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO movies(title, genre, language, duration, price, show_time) VALUES(?,?,?,?,?,?)")) {
                for (Movie m : movies) {
                    ps.setString(1,m.title); ps.setString(2,m.genre); ps.setString(3,m.language);
                    ps.setInt(4,m.duration); ps.setDouble(5,m.price); ps.setString(6,m.showTime); ps.addBatch();
                }
                ps.executeBatch();
            }
            try (java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO bookings(id, movie_title, seats, customer, booked_at, total) VALUES(?,?,?,?,?,?)")) {
                for (Booking b : bookings) {
                    ps.setString(1,b.id); ps.setString(2,b.movieTitle); ps.setString(3,b.seats);
                    ps.setString(4,b.customer); ps.setString(5,b.bookedAt); ps.setDouble(6,b.total); ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not save database data: " + ex.getMessage(), "CineBook database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new Main().setVisible(true);
        });
    }
}
