package Common;

import java.util.List;

public class Command extends AbstractMessage {
    private String command;
    private String fileName;
    private List<String> al;

    public Command(String command, String fileName) {
        this.command = command;
        this.fileName = fileName;
    }

    public Command(String command) {
        this.command = command;
    }

    public Command(String command, List<String> al) {
        this.command = command;
        this.al = al;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCommand() {
        return command;
    }

    public List<String> getAl() {
        return al;
    }
}
