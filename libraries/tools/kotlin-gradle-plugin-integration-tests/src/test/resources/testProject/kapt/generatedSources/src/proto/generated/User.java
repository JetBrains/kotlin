public final class User {
  private User() {}

  public static final class UserInfo {

    private UserInfo() {
      name_ = "";
      email_ = "";
    }

    private volatile String name_;

    public String getName() {
      return name_;
    }

    public void setName(String name) {
      name_ = name;
    }

    private volatile String email_;

    public String getEmail() {
      return email_;
    }

    public void setEmail(String email) {
      email_ = email;
    }
  }
}
