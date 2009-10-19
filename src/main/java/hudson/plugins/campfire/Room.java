package hudson.plugins.campfire;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class Room {
    private transient Campfire campfire;
    private String name;
    private String id;

    public Room(Campfire cf, String name, String id) {
        super();
        this.campfire = cf;
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

    public void join() throws IOException {
        campfire.get("room/" + id);
    }

    public void leave() throws IOException {
        campfire.post("room/" + id + "/leave", new HashMap<String, String>());
    }

    public void speak(String message) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("message", message);
        params.put("t", String.valueOf(new Date().getTime()));
        System.out.println("Speaking:" + message);
        campfire.post("room/" + id + "/speak", params);
    }
}
