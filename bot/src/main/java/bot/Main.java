package bot;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;

public class Main {
    private static BotListener bot;
    private final static int SAVEFREQUENCY = 120000; //frequency to save information, in milliseconds (every 2 minutes)
    private static class SaveState extends TimerTask { //saves information on the bot
        public void run() {
            bot.save();
        }
    }
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        System.out.println("Enter discord token:");//prompts for discord token to connect to discord
        String token=sc.next();
        DiscordApi api = new DiscordApiBuilder().setToken(token) //connects bot to discord api
            .setAllIntentsExcept(Intent.GUILD_PRESENCES).login().join();
        bot = new BotListener(api); //creates new message listener and adds it to the current bot
        api.addListener(bot);
        DmojCfApi.updateCache(); //updates the cache of dmoj and codeforces problems
        api.getOwner().join().sendMessage("Starting the bot"); //sends startup message
        System.out.println("Bot started");
        Timer timer = new Timer(); //sets a timer to save bot data automatically
        timer.schedule(new SaveState(), 0, SAVEFREQUENCY);
        sc.close();
    }
}