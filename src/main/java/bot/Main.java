package bot;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;

public class Main {
    private static BotListener bot;
    private static class Save extends TimerTask { //saves information on the bot
        public void run() {
            bot.save();
        }
    }
    private static class Update1 extends TimerTask {
        public void run() {
            bot.update1();
        }
    }
    private static class Update2 extends TimerTask {
        public void run() {
            bot.update2();
        }
    }
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        System.out.println("Enter discord token:");//prompts for discord token to connect to discord
        String token=System.getenv("token");// sc.next();
        sc.close();
        DiscordApi api = new DiscordApiBuilder().setToken(token) //connects bot to discord api
            .setAllIntentsExcept(Intent.GUILD_PRESENCES).login().join();
        bot = new BotListener(api); //creates new message listener and adds it to the current bot
        api.addListener(bot);
        Timer timer = new Timer(); //sets a timer to save bot data automatically
        timer.schedule(new Save(), 0, 60000);//1 minute
        timer.schedule(new Update1(), 0, 3600000);//1 hour
        timer.schedule(new Update2(), 0, 604800000);//1 week
        api.getOwner().join().sendMessage("Starting the bot"); //sends startup message
        System.out.println("Bot started");
    }
}