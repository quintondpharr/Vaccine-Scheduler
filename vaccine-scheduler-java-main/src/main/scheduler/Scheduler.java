package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) { // exact same as createCaregiver but with Patients
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }

        // check 3: check if the password is strong
        if (!isStrongPassword(password)) {
            System.out.println("Password is not strong enough, try again!\n" +
                    "Please include at least 8 characters.\n" +
                    "A mixture of both uppercase and lowercase letters.\n" +
                    "A mixture of letters and numbers.\n" +
                    "Inclusion of at least one special character, from “!”, “@”, “#”, “?”.\n");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }

        // check 3: check if the password is strong
        if (!isStrongPassword(password)) {
            System.out.println("Password is not strong enough, try again!\n" +
                    "Please include at least 8 characters.\n" +
                    "A mixture of both uppercase and lowercase letters.\n" +
                    "A mixture of letters and numbers.\n" +
                    "Inclusion of at least one special character, from “!”, “@”, “#”, “?”.\n");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) { // exact same as usernameExistsCaregiver
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?"; // change to Patients
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }

        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }

        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {

        // if no one is logged in, print an error message and return
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        // Check for the correct number of tokens
        if (tokens.length != 2) {
            System.out.println("Invalid input");
            return;
        }

        // Get the date from the tokens
        String date = tokens[1];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {

            // Convert the date string to a Date object for the sql query
            Date d = Date.valueOf(date);

            // Get the caregivers that are available for the date along with all the vaccine information
            String getCaregivers = "SELECT a.Username, v.Name, v.Doses " +
                    "FROM Availabilities a " +
                    "JOIN Caregivers c ON a.Username = c.Username " + // Join to get the caregiver information
                    "LEFT JOIN Vaccines v ON 1=1 " + // Left join to get all the vaccines even if there are no doses
                    "WHERE a.Time = ? " +
                    "ORDER BY a.Username";
            PreparedStatement statement = con.prepareStatement(getCaregivers);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.isBeforeFirst()) { // Check if there are any results
                System.out.println("No caregivers available for this date.");
                return;
            }

            // Print the results
            while (resultSet.next()) {
                String caregiverUsername = resultSet.getString(1);
                String vaccineName = resultSet.getString(2);
                int doses = resultSet.getInt(3);

                System.out.println("Caregiver: " + caregiverUsername +
                        ", Vaccine: " + vaccineName +
                        ", Available Doses: " + doses);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for caregivers");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

    }

    private static void reserve(String[] tokens) {
        // Check if a patient is logged in
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }

        // Check for correct number of tokens
        if (tokens.length != 3) {
            System.out.println("Invalid input");
            return;
        }

        String date = tokens[1];
        String vaccineName = tokens[2];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            // Convert the date string to a Date object
            Date d = Date.valueOf(date);

            // Check if the vaccine is available
            if (!isVaccineAvailable(vaccineName, con)) {
                System.out.println("Vaccine not available.");
                return;
            }

            // Find an available caregiver
            String caregiver = findAvailableCaregiver(d, con);
            if (caregiver == null) {
                System.out.println("No caregivers available on this date.");
                return;
            }

            // Create an appointment
            createAppointment(currentPatient.getUsername(), caregiver, d, vaccineName, con);

            // Update vaccine doses
            updateVaccineDoses(vaccineName, -1, con);  // assuming each reservation uses one dose

            System.out.println("Appointment reserved for " + currentPatient.getUsername() + " with " + caregiver + " on " + date + " for " + vaccineName + " vaccine.");

        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when reserving appointment");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    // helper functions for reserve to check if a vaccine is available
    private static boolean isVaccineAvailable(String vaccineName, Connection con) throws SQLException {
        String query = "SELECT Doses FROM Vaccines WHERE Name = ?";
        PreparedStatement statement = con.prepareStatement(query);
        statement.setString(1, vaccineName);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            int doses = resultSet.getInt("Doses");
            return doses > 0;
        }

        return false;
    }

    // helper function for reserve to find an available caregiver
    private static String findAvailableCaregiver(Date d, Connection con) throws SQLException {

        // Get the caregivers that are available for the date along with all the vaccine information
        // making sure that the caregiver is not already booked for that date
        String query = "SELECT Username FROM Availabilities WHERE Time = ? " +
                "AND Username NOT IN (SELECT CaregiverUsername FROM Appointment WHERE AppointmentDate = ?)";
        PreparedStatement statement = con.prepareStatement(query);
        statement.setDate(1, d); // set the date for the first ? in the query
        statement.setDate(2, d); // set the date for the second ? in the query
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            return resultSet.getString("Username");
        }

        return null;
    } // testPatient 12345678Upperlower!

    // helper function for reserve to create an appointment
    private static void createAppointment(String patientUsername, String caregiverUsername, Date date, String vaccineName, Connection con) throws SQLException {
        String insertAppointment = "INSERT INTO Appointment (AppointmentDate, CaregiverUsername, PatientUsername, VaccineName) VALUES (?, ?, ?, ?)";
        PreparedStatement statement = con.prepareStatement(insertAppointment);
        statement.setDate(1, date);
        statement.setString(2, caregiverUsername);
        statement.setString(3, patientUsername);
        statement.setString(4, vaccineName);
        statement.executeUpdate();
    }

    // helper function for reserve to update the vaccine doses
    private static void updateVaccineDoses(String vaccineName, int doseChange, Connection con) throws SQLException {
        String updateDoses = "UPDATE Vaccines SET Doses = Doses + ? WHERE Name = ?";
        PreparedStatement statement = con.prepareStatement(updateDoses);
        statement.setInt(1, doseChange);
        statement.setString(2, vaccineName);
        statement.executeUpdate();
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            String query;
            if (currentPatient != null) {
                query = "SELECT a.AppointmentID, a.VaccineName, a.AppointmentDate, c.Username " +
                        "FROM Appointment a " +
                        "JOIN Caregivers c ON a.CaregiverUsername = c.Username " +
                        "WHERE a.PatientUsername = ? " +
                        "ORDER BY a.AppointmentID";
            } else {
                query = "SELECT a.AppointmentID, a.VaccineName, a.AppointmentDate, p.Username " +
                        "FROM Appointment a " +
                        "JOIN Patients p ON a.PatientUsername = p.Username " +
                        "WHERE a.CaregiverUsername = ? " +
                        "ORDER BY a.AppointmentID";
            }

            PreparedStatement statement = con.prepareStatement(query);

            if (currentPatient != null) {
                statement.setString(1, currentPatient.getUsername());
            } else {
                statement.setString(1, currentCaregiver.getUsername());
            }

            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.isBeforeFirst()) {
                System.out.println("No appointments found.");
                return;
            }

            while (resultSet.next()) {
                int appointmentID = resultSet.getInt(1);
                String vaccineName = resultSet.getString(2);
                Date appointmentDate = resultSet.getDate(3);
                String appointmentUsername = resultSet.getString(4);

                System.out.println("Appointment ID: " + appointmentID +
                        ", Vaccine Name: " + vaccineName +
                        ", Appointment Date: " + appointmentDate +
                        ", Username: " + appointmentUsername);
            }

        } catch (SQLException e) {
            System.out.println("Error occurred when retrieving appointments.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        try {
            if (currentCaregiver != null) {
                currentCaregiver = null;
            } else if (currentPatient != null) {
                currentPatient = null;
            } else {
                System.out.println("Please login first!");
                return;
            }
            System.out.println("Successfully logged out!");
        } catch (Exception e) {
            System.out.println("Please try again!");
        }
    }

    /**
     * Extra credit: check if a password is strong
     * @param password
     * @return
     */
    public static boolean isStrongPassword(String password) {
        if (password.length() < 8) {
            return false;
        }

        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {hasUpper = true;}
            else if (Character.isLowerCase(ch)) {hasLower = true;}
            else if (Character.isDigit(ch)) {hasDigit = true;}
            else if (ch == '!' || ch == '@' || ch == '#' || ch == '?') {hasSpecial = true;}

            if (hasUpper && hasLower && hasDigit && hasSpecial) {
                return true;
            }
        }
        return false;
    }
}



