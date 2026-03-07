package com.japtrack.project.dto.request;


public class UserRequest {

    private String userName;
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String password;


    public UserRequest() {}


    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserFirstName() { return userFirstName; }
    public void setUserFirstName(String userFirstName) { this.userFirstName = userFirstName; }

    public String getUserLastName() { return userLastName; }
    public void setUserLastName(String userLastName) { this.userLastName = userLastName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}










