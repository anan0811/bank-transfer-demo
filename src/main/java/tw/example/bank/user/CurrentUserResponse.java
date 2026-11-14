package tw.example.bank.user;

public record CurrentUserResponse(String id, String name, String role) {
    public static CurrentUserResponse from(DemoUser user) {
        return new CurrentUserResponse(user.getLoginId(), user.getDisplayName(), user.getRole());
    }
}
