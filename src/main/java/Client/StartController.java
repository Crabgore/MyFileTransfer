package Client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class StartController implements Initializable {

    @FXML
    TextField loginField;

    @FXML
    VBox startNode;

    @FXML
    TextField passField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void changeSceneToMain(){
        try {
            String fxmlFile = "/fxml/main.fxml";
            FXMLLoader loader = new FXMLLoader();
            Parent mainScene = (Parent) loader.load(getClass().getResourceAsStream(fxmlFile));
            ((Stage) startNode.getScene().getWindow()).setScene(new Scene(mainScene));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
