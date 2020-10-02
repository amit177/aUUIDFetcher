import org.bukkit.plugin.java.JavaPlugin;
import systems.amit.aUUIDFetcher.spigot.aUUIDFetcher;

import java.util.UUID;

public class test extends JavaPlugin {

    private aUUIDFetcher uuidFetcher;

    @Override
    public void onEnable(){
        uuidFetcher = new aUUIDFetcher(this);

        uuidFetcher.fetchName(UUID.fromString("1a28d37e-a3e1-49d0-a268-c971c59ff177"), (name) -> {
            System.out.println("ASYNC: " + name);
        });
        uuidFetcher.fetchUUID("amit177", (uuid) -> {
            System.out.println("ASYNC: " + uuid.toString());
        });

        System.out.println("BLOCKING: " + uuidFetcher.fetchNameBlocking(UUID.fromString("1a28d37e-a3e1-49d0-a268-c971c59ff177")));
        System.out.println("BLOCKING: " + uuidFetcher.fetchUUIDBlocking("amit177"));
    }
}