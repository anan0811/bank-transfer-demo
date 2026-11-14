package tw.example.bank.user;

public record DemoAccountResponse(String id, String name) {
    public static DemoAccountResponse from(DemoUser user) {
        return new DemoAccountResponse(user.getLoginId(), user.getDisplayName());
    }
}
