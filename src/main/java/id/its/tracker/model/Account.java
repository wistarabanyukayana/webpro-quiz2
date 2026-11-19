package id.its.tracker.model;

public record Account(long id, String username, String fullName, String email, String passwordHash,
                      String lastLogin, long initialBalance, String rememberToken, int sessionVersion) {
}
