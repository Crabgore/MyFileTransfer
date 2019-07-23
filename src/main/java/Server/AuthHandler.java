package Server;

import Common.Command;
import Common.UserInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authOk = false;

    private static Connection connection;
    private static Statement stmt;

    private static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:mainDB.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void addUser(String login, String pass) {
        String sql = String.format("INSERT INTO main (login, password)" +
                " VALUES ('%s', '%s')", login, pass.hashCode());
        try {
            stmt.execute(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkLogin(String login){
        String check = String.format("SELECT login from main" +
                " WHERE login = '%s'", login);
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery(check);
            if (rs.next()) {
                if (rs.getString(1).equals(login))
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static boolean userInfo(String login, String pass) {
        String sql = String.format("SELECT password FROM main\n" + "WHERE login = '%s'", login);

        int myHash = pass.hashCode();

        ResultSet rs = null;
        try {
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                int dbHash = rs.getInt(1);

                if (myHash == dbHash) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        UserInfo userInfo = (UserInfo) msg;
        String[] strings = userInfo.getUserInfo().split(" ");

        if (authOk){
            ctx.fireChannelRead(msg);
            return;
        }

        if (strings[0].equals("/addUser")){
            connect();
            if (checkLogin(strings[1])){
                addUser(strings[1], strings[2]);
                Files.createDirectories(Paths.get("storages/" + strings[1]));
                Command cmd = new Command("loginOk");
                ctx.writeAndFlush(cmd);
            } else {
                Command cmd = new Command("loginExist");
                ctx.writeAndFlush(cmd);
            }
            disconnect();
        }

        if (strings[0].equals("/auth")){
            connect();
            if (userInfo(strings[1], strings[2])){
                authOk = true;
                ctx.pipeline().addLast(new MainHandler(strings[1]));
                Command cmd = new Command("authOk");
                ctx.writeAndFlush(cmd);
                ctx.pipeline().remove(this);
            } else {
                Command cmd = new Command("authFailed");
                ctx.writeAndFlush(cmd);
            }
            disconnect();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
