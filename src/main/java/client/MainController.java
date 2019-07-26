package client;

import common.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    HBox rootNode;

    @FXML
    VBox startNode;

    @FXML
    TextField status;

    @FXML
    TextField passField;

    @FXML
    TextField loginField;

    @FXML
    Button login;

    @FXML
    Button register;

    @FXML
    ListView<String> clientFilesList;

    @FXML
    ListView<String> serverFilesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Network.start();
        status.setFocusTraversable(false);
        loginField.setFocusTraversable(true);
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Path path = Paths.get("client_storage/" + fm.getFilename());
                        if (!Files.exists(path)) {
                            Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE_NEW);
                       } else{
                            int count = 1;
                            while (true) {
                                String[] strings = fm.getFilename().split("");
                                String title = nameBuilder(strings, count);
                                Path name = Paths.get("client_storage/" + title);
                                if (Files.exists(name)) count++;
                                else {
                                    Files.write(Paths.get("client_storage/" + title), fm.getData(), StandardOpenOption.CREATE);
                                    break;
                                }
                            }
                        }
                        refreshLocalFilesList();
                    }
                    if (am instanceof Command){
                        Command cmd = (Command) am;
                        if (cmd.getCommand().equals("authOk")){
                            startNode.setVisible(false);
                            startNode.setManaged(false);
                            rootNode.setVisible(true);
                            rootNode.setManaged(true);
                            loginField.clear();
                            passField.clear();
                            Network.sendMsg(new Command("update"));
                            refreshLocalFilesList();
                        }
                        if (cmd.getCommand().equals("authFailed")){
                            status.setText("Неверный логин или пароль!");
                        }
                        if (cmd.getCommand().equals("loginExist")){
                            status.setText("Такой логин уже существует!");
                        }
                        if (cmd.getCommand().equals("loginOk")){
                            status.setText("Вы успешно зарегестрированы!");
                        }
                        if (cmd.getCommand().equals("update")){
                            refreshCloudFilesList(cmd.getAl());
                        }
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void pressOnDownloadBtn() {
        if (serverFilesList.getSelectionModel().getSelectedItem() != null) {
            Network.sendMsg(new FileRequest(serverFilesList.getSelectionModel().getSelectedItem()));
        }
    }

    public void pressOnUploadBtn() {
        if (clientFilesList.getSelectionModel().getSelectedItem() != null) {
            try {
                FileMessage fm = new FileMessage(Paths.get("client_storage/" + clientFilesList.getSelectionModel().getSelectedItem()));
                if (fm.getData().length <= 2146435072) Network.sendMsg(fm);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                clientFilesList.getItems().clear();
                Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> clientFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void refreshCloudFilesList(List<String> files) {
        updateUI(() -> {
            serverFilesList.getItems().clear();
            for (String s : files) {
                serverFilesList.getItems().add(s);
            }
        });
    }

    public void clientDelete() {
        if (clientFilesList.getSelectionModel().getSelectedItem() != null) {
            try {
                Files.delete(Paths.get("client_storage/" + clientFilesList.getSelectionModel().getSelectedItem()));
                refreshLocalFilesList();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void serverDelete() {
        if (serverFilesList.getSelectionModel().getSelectedItem() != null) {
            Network.sendMsg(new Command("del", serverFilesList.getSelectionModel().getSelectedItem()));
        }
    }

    private static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void pressOnLogin() {
        if (loginField.getText().equals("")|| passField.getText().equals("")) {
            status.setText("Введите логи и пароль");
        } else
            Network.sendMsg(new UserInfo("/auth" + " " + loginField.getText() + " " + passField.getText()));
    }

    public void pressOnRegister() {
        if (loginField.getText().equals("")|| passField.getText().equals("")){
            status.setText("Введите логи и пароль");
        } else
            Network.sendMsg(new UserInfo("/addUser" + " " + loginField.getText() + " " + passField.getText()));
    }

    public void exit() {
        Network.sendMsg(new Command("exit"));
        startNode.setVisible(true);
        startNode.setManaged(true);
        rootNode.setVisible(false);
        rootNode.setManaged(false);
    }

    private String nameBuilder(String[] strings, int count){
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(strings));
        arrayList.add(arrayList.size()-4, "(" + count + ")");
        StringBuilder builder = new StringBuilder(arrayList.size());
        for(String s: arrayList) {
            builder.append(s);
        }
        return builder.toString();
    }
}