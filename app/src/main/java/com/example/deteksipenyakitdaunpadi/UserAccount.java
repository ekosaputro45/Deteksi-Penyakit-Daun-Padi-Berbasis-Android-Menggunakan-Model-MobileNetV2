package com.example.deteksipenyakitdaunpadi;

public class UserAccount {
    public final String email;
    public final String username;
    public final String profileImageUri;

    public final String fullName;
    public final String phoneNumber;
    public final String gender;
    public final String dateOfBirth;

    public UserAccount(String email, String username, String profileImageUri, String fullName, String phoneNumber, String gender, String dateOfBirth) {
        this.email = email;
        this.username = username;
        this.profileImageUri = profileImageUri;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
    }
}
