package services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.EventType;
import fr.tunaki.stackoverflow.chat.event.UserMentionedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JsonUtils;

import javax.json.Json;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by bhargav.h on 18-May-17.
 */
public class Runner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);
    private Instant previousRunTime ;
    private Room rooms[];
    private ScheduledExecutorService executorService;

    public Runner(Room[] rooms){
        this.rooms = rooms;
        this.previousRunTime = Instant.now().minus(5, ChronoUnit.MINUTES);
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void startDetector(){
        for (Room room: rooms) {
            room.addEventListener(EventType.USER_MENTIONED, event -> mention(room, event, false));
        }
        Runnable runner = () -> runCommentBotOnce(rooms);
        executorService.scheduleAtFixedRate(runner, 0, 2, TimeUnit.MINUTES);
    }

    private void mention(Room room, UserMentionedEvent event, boolean b) {
        String message = event.getMessage().getPlainContent();
        LOGGER.debug(message);
        if(message.toLowerCase().contains("help")){
            room.send("I'm an experimental bot, trying to detect all new comments posted on Interpersonal.SE and post them to a chatroom for monitering.");
        }
        else if(message.toLowerCase().contains("alive")){
            room.send("Yep");
        }
    }

    public void restartMonitor(){
        endDetector();
        startDetector();
    }

    private void endDetector(){
        LOGGER.debug("Shutting down");
        executorService.shutdown();
    }

    private void leaveRoom(Room room){
        LOGGER.debug("Leaving room "+room.getRoomId());
        room.leave();
    }

    private void shutdownBot(){
        for (Room room: rooms) {
            leaveRoom(room);
        }
        endDetector();
        System.exit(0);
    }

    private void runCommentBotOnce(Room []rooms){
        List<JsonObject> comments = getDataFromApi();
        String desc = "[ [GetAllTehCommentz](https://git.io/vbxFf) ]";
        for (Room room: rooms) {
            for (JsonObject comment : comments) {
                room.send(desc + " New comment:");
                room.send(comment.get("link").getAsString());
            }
        }
    }

    private List<JsonObject> getDataFromApi() {
        List<JsonObject> commentObjs = new ArrayList<>();
        try{
            String url = "http://api.stackexchange.com/2.2/comments";
            String apiKey = "kmtAuIIqwIrwkXm1*p3qqA((";
            int number = 1;
            JsonObject json;
            do {
                 json = JsonUtils.get(url,
                        "sort", "creation",
                        "site", "interpersonal",
                        "pagesize", "100",
                        "page", "" + number,
                        "fromdate", Long.toString(previousRunTime.getEpochSecond()),
                        "order", "desc",
                        "filter", "!-*f(6skrN-SN",
                        "key", apiKey);

                if (json.has("items")) {
                    for (JsonElement element : json.get("items").getAsJsonArray()) {
                        JsonObject object = element.getAsJsonObject();
                        commentObjs.add(object);
                    }
                }
                JsonUtils.handleBackoff(LOGGER,json);
                number++;
                LOGGER.debug(json.toString());
            }
            while (json.get("has_more").getAsBoolean());
            previousRunTime = Instant.now();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return commentObjs;
    }
}
