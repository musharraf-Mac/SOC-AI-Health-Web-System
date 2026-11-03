package com.example.doc_booking.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection = "users")
public class RegisteredUser {
    @Id private String id;
    private String fullname;
    private String email;
    private String role;
    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getFullname() { return fullname; }
    public void setFullname(String value) { fullname = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getRole() { return role; }
    public void setRole(String value) { role = value; }
}
