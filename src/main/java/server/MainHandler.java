package server;

import common.Command;
import common.FileMessage;
import common.FileRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private String userName = null;
    private String storage = null;
    private List<String> results = new ArrayList<>();

    MainHandler(String userName){
        this.userName = userName;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        storage = "storages/" + userName + "/";
        try {
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get(storage + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get(storage + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                }
            }
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                Path path = Paths.get(storage + fm.getFilename());
                if (!Files.exists(path)) {
                    Files.write(Paths.get(storage + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                } else{
                    int count = 1;
                    while (true){
                        String[] strings = fm.getFilename().split("");
                        String title = nameBuilder(strings, count);
                        Path name = Paths.get(storage + title);
                        if (Files.exists(name)) count++;
                        else {
                            Files.write(Paths.get(storage + title), fm.getData(), StandardOpenOption.CREATE);
                            break;
                        }
                    }
                }
                update();
                Command cmd = new Command("update", results);
                ctx.writeAndFlush(cmd);
            }
            if (msg instanceof Command) {
                Command cmd = (Command) msg;
                if (cmd.getCommand().equals("del")) {
                    Files.delete(Paths.get(storage + cmd.getFileName()));
                    update();
                    Command del = new Command("update", results);
                    ctx.writeAndFlush(del);
                }
                if (cmd.getCommand().equals("update")){
                    update();
                    Command upd = new Command("update", results);
                    ctx.writeAndFlush(upd);
                }
                if (cmd.getCommand().equals("exit")){
                    ctx.pipeline().addLast(new AuthHandler());
                    ctx.pipeline().remove(this);
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void update(){
        results.clear();
        File[] files = new File(storage).listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isFile()) {
                results.add(file.getName());
            }
        }
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
