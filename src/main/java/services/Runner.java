package services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.EventType;
import fr.tunaki.stackoverflow.chat.event.MessagePostedEvent;
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
    private Room room;
    private ScheduledExecutorService executorService;

    public Runner(Room room){
        this.room = room;
        this.previousRunTime = Instant.now().minus(5, ChronoUnit.MINUTES);
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void startDetector(){
        room.addEventListener(EventType.USER_MENTIONED,event->mention(room, event, false));

        Runnable runner = () -> runEditBotOnce(room);
        executorService.scheduleAtFixedRate(runner, 0, 5, TimeUnit.MINUTES);
    }

 private void mention(Room room, UserMentionedEvent event, boolean b) {
        String message = event.getMessage().getPlainContent();
        if(message.toLowerCase().contains("help")){
            room.send("I'm an experimental bot");
        }
        else if(message.toLowerCase().contains("alive")){
            room.send("Yep");
        }
    }

    public void restartMonitor(){
        endDetector();
        startDetector();
    }

    public void endDetector(){
        executorService.shutdown();
    }

    public void runEditBotOnce(Room room){
        try{
            String desc = "[ [GetAllTehCommentz](https://git.io/vbxFf) ]";
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
                        if (object.get("body_markdown").getAsString().matches(".*.*")) {
                            room.send(desc + " New comment:");
                            room.send(object.get("link").getAsString());
                        }
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

    }
}
