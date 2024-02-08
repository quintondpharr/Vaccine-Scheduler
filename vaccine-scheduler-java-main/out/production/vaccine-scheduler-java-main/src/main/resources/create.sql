CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Appointment (
    AppointmentID int NOT NULL AUTO_INCREMENT,
    AppointmentDate date,
    CaregiverUsername varchar(255) REFERENCES Caregivers,
    PatientUsername varchar(255) REFERENCES Patients,
    VaccineName varchar(255) REFERENCES Vaccines,
    PRIMARY KEY (AppointmentID)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);