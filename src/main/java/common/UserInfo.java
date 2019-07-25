package common;

public class UserInfo extends AbstractMessage {
    private String userInfo;

    public UserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public String getUserInfo() {
        return userInfo;
    }
}
