@Document(collection = "test_booked_patients")
public class Patient {
    @Id
    private String id;
    private String name;
    private String email;
    private int age;
    private String disease;
    // getters & setters
}


