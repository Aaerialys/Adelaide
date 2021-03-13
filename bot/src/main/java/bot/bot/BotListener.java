package bot;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

public class BotListener implements MessageCreateListener { //this class receives messages and responds to commands from users
	private final long AAERIA=543096067059744800L;
    private final int MAXLENGTH = 1990; //max message length on discord
    private final int PERMISSIONS = 1073835072; //the required discord permissions for the bot;
    private final HashMap < String, String > DMOJEXT =new HashMap<String,String>();
    private final HashMap < String, String > CFEXT =new HashMap<String,String>();
    private static HashMap<String,Long> userFromDmoj=new HashMap<String,Long>(),userFromCf=new HashMap<String,Long>();
	private long HOME=-1;
    private int emojiCycleNumber=0; //0 for no cycling
    private String prefix = "!"; //all bot commands start with this prefix
    private HashMap < Long, User > users = new HashMap < Long, User > (); //list of users using the bot
    private HashMap < String, GlobalProblem > problems = new HashMap < String, GlobalProblem > (); //list of problems that have been voted on
    private DiscordApi api; //discord api connection	
    private HashSet<String> adminRole=new HashSet<String>();
    boolean stopDownload=true;
    long currentDownload=-1;
    long prevTime=-1;
    private HashMap<String,Long> emojiLastOccurrence=new HashMap<String,Long>();
    
    private long author;
    private String fullInput;
    private String[] input;
    private MessageCreateEvent event;
    
    public void save() { //saves important info stored by the bot
    	if(new Date().getTime()-prevTime>=130000) return;
        FileOutputStream f;
        try {
            f = new FileOutputStream(new File("data.txt")); //opens data storage file, or creates one if it doesn't exist
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(users); //saves list of users
            o.writeObject(problems); //saves list of problems with votes
            o.writeLong(HOME);
            o.writeInt(emojiCycleNumber);
            o.writeObject(prefix);
            o.writeObject(adminRole);
            o.writeObject(userFromDmoj);
            o.writeObject(userFromCf);
            o.close();
            f.close();
        } catch (IOException e) {
            System.err.println("Unable to save data to file");
            e.printStackTrace();
        }
    }
    public void update1() {
        DmojCfApi.updateCache(); //updates the cache of dmoj and codeforces problems
        for(User user:users.values()) {
        	user.setCfName(user.getCfName(), "");
        	user.setDmojName(user.getDmojName(), "");
        	userFromDmoj.put(user.getDmojName(),user.getId());
        	userFromCf.put(user.getCfName(),user.getId());
        	try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    public BotListener(DiscordApi api) { //BotListener constructor
        this.api = api;
        try { //retrieves data from the last time the bot was running
        	/*InputStream in = new URL("https://raw.githubusercontent.com/Aaerialys/Adelaide/main/data.txt").openStream();
        	Files.copy(in, Paths.get("data.txt"), StandardCopyOption.REPLACE_EXISTING);
        	in.close();*/
            FileInputStream fi = new FileInputStream(new File("data.txt")); //gets data from storage file*/
            ObjectInputStream oi = new ObjectInputStream(fi);
            users = (HashMap < Long, User > ) oi.readObject(); //retrieves list of users
            problems = (HashMap < String, GlobalProblem > ) oi.readObject(); //retrieves list of problems
            HOME=oi.readLong();
            emojiCycleNumber=oi.readInt();
            prefix=(String)oi.readObject();
            adminRole=(HashSet<String>)oi.readObject();
            userFromDmoj=(HashMap<String,Long>)oi.readObject();
            userFromCf=(HashMap<String,Long>)oi.readObject();
            System.out.println("data restored");
            oi.close();
            fi.close();
        } catch (IOException | ClassNotFoundException e) { //if the storage file does not yet exist, or cannot be read
            System.err.println("Unable to retrieve stored data file");
            e.printStackTrace();
        }
        DMOJEXT.put("ADA",".ada");DMOJEXT.put("AWK",".awk");DMOJEXT.put("BF",".bf");DMOJEXT.put("C",".c");
        DMOJEXT.put("C11",".c");DMOJEXT.put("CBL",".cbl");DMOJEXT.put("CCL",".ccl");DMOJEXT.put("CLANG",".c");
        DMOJEXT.put("CLANGX",".c");DMOJEXT.put("COFFEE",".coffee");DMOJEXT.put("CPP03",".cpp");DMOJEXT.put("CPP11",".cpp");
        DMOJEXT.put("CPP14",".cpp");DMOJEXT.put("CPP17",".cpp");DMOJEXT.put("D",".d");DMOJEXT.put("DART",".dart");
        DMOJEXT.put("F95",".f95");DMOJEXT.put("FORTH",".forth");DMOJEXT.put("GAS32",".s");DMOJEXT.put("GAS64",".s");
        DMOJEXT.put("GASARM",".s");DMOJEXT.put("GO",".go");DMOJEXT.put("GROOVY",".groovy");DMOJEXT.put("HASK",".hs");
        DMOJEXT.put("ICK",".ick");DMOJEXT.put("JAVA11",".java");DMOJEXT.put("JAVA8",".java");DMOJEXT.put("KOTLIN",".kt");
        DMOJEXT.put("LUA",".lua");DMOJEXT.put("MONOCS",".mono");DMOJEXT.put("MONOFS",".mono");DMOJEXT.put("MONOVB",".mono");
        DMOJEXT.put("NASM",".asm");DMOJEXT.put("NASM64",".asm");DMOJEXT.put("NIM",".nim");DMOJEXT.put("OBJC",".m");
        DMOJEXT.put("OCAML",".ml");DMOJEXT.put("OCTAVE",".m");DMOJEXT.put("PAS",".pas");DMOJEXT.put("PERL",".pl");
        DMOJEXT.put("PHP",".php");DMOJEXT.put("PIKE",".pike");DMOJEXT.put("PRO",".pro");DMOJEXT.put("PY2",".py");
        DMOJEXT.put("PY3",".py");DMOJEXT.put("PYPY",".py");DMOJEXT.put("PYPY2",".py");DMOJEXT.put("PYPY3",".py");
        DMOJEXT.put("RKT",".rkt");DMOJEXT.put("RUBY18",".rb");DMOJEXT.put("RUBY2",".rb");DMOJEXT.put("RUST",".rs");
        DMOJEXT.put("SBCL",".lisp");DMOJEXT.put("SCALA",".sc");DMOJEXT.put("SCM",".scm");DMOJEXT.put("SED",".sed");
        DMOJEXT.put("SWIFT",".swift");DMOJEXT.put("TCL",".tcl");DMOJEXT.put("TEXT",".txt");DMOJEXT.put("TUR",".t");
        DMOJEXT.put("V8JS",".js");DMOJEXT.put("VC",".c");DMOJEXT.put("ZIG",".zig");
        CFEXT.put("GNU C",".c");CFEXT.put("GNU C11",".c");CFEXT.put("Clang++17 Diagnostics",".cpp");CFEXT.put("GNU C++",".cpp");
        CFEXT.put("GNU C++11",".cpp");CFEXT.put("GNU C++14",".cpp");CFEXT.put("GNU C++17",".cpp");CFEXT.put("GNU C++17 Diagnostics",".cpp");
        CFEXT.put("MS C++",".cpp");CFEXT.put("Mono C#",".cs");CFEXT.put("D",".d");CFEXT.put("Go",".go");CFEXT.put("Haskell",".hs");
        CFEXT.put("Java 8",".java");CFEXT.put("Kotlin",".kt");CFEXT.put("Ocaml",".ml");CFEXT.put("Delphi",".dpr");CFEXT.put("FPC",".pas");
        CFEXT.put("PascalABC.NET",".pas");CFEXT.put("Perl",".pl");CFEXT.put("PHP",".php");CFEXT.put("Python 2",".py");CFEXT.put("Python 3",".py");
        CFEXT.put("PyPy 2",".py");CFEXT.put("PyPy 3",".py");CFEXT.put("Ruby",".rb");CFEXT.put("Rust",".rs");CFEXT.put("Scala",".scala");
        CFEXT.put("JavaScript",".js");CFEXT.put("Node.js",".js");CFEXT.put("Q#",".qs"); CFEXT.put("GNU C++17 (64)", ".cpp"); CFEXT.put("Java 11",".java");
    }
    
    private Long parseUser2(String input, Server s) { //recieves an input string and the bot's current server, converts input into a discord user id
        try {    
    	return api.getUserById(input.replaceAll("[<>@!]", "")).join().getId();
        } catch(CompletionException e) {}
    	if (api.getCachedUserById(input.replaceAll("[<>@!]", "")).isPresent()) //checks if the input could be an id number or mention (eg. <@543096067059744800>)
            return api.getCachedUserById(input.replaceAll("[<>@!]", "")).get().getId();
        if (input.contains("#") && api.getCachedUserByDiscriminatedNameIgnoreCase(input).isPresent()) //checks if the input could be a username with discriminator (eg. aaeria#0001)
            return api.getCachedUserByDiscriminatedNameIgnoreCase(input).get().getId();
        if (!api.getCachedUsersByNameIgnoreCase(input).isEmpty()) //checks if the input could be a username without discriminator (eg. aaeria)
            return api.getCachedUsersByNameIgnoreCase(input).iterator().next().getId();
        if (s != null && !api.getCachedUsersByNicknameIgnoreCase(input, s).isEmpty()) //checks if the input could be the nickname of a user in the server (eg. bob)
            return api.getCachedUsersByNicknameIgnoreCase(input, s).iterator().next().getId();
        if(userFromDmoj.containsKey(input))
        	return userFromDmoj.get(input);
        if(userFromCf.containsKey(input))
        	return userFromCf.get(input);
        return null; //if the input is none of these, it doesn't contain a valid discord user
    }
    private Long parseUser(String input,Server s) {
    	Long user=parseUser2(input,s);
    	if(user!=null&&!users.containsKey(user)) users.put(user, new User(user));
    	return user;
    }
    
    private void updateEmotes(Server server) {
    	if(!server.canYouManageEmojis()) return;
    	HashSet<String> emotes=new HashSet<String>();
    	for(int prev=fullInput.indexOf(':',0),cur=fullInput.indexOf(':',prev+1),cnt=0; cur!=-1;prev=cur,cur=fullInput.indexOf(':',prev+1)) {
    		emotes.add(fullInput.substring(prev+1,cur));
    		if(++cnt>=5) break;
    	}
		ArrayList<KnownCustomEmoji> temp= new ArrayList<KnownCustomEmoji>(server.getCustomEmojis()),emoteS=new ArrayList<KnownCustomEmoji>(),emoteA=new ArrayList<KnownCustomEmoji>();
		for(KnownCustomEmoji cur:temp) {
			if(cur.isAnimated()) emoteA.add(cur);
			else emoteS.add(cur);
		}
		int max=server.getBoostLevel().getId();
		if(max==3) max++;
		max=50+50*max;
    	for(String emote:emotes) {
    		emojiLastOccurrence.put(emote, new Date().getTime());
    		if(server.getCustomEmojisByName(emote).isEmpty()) { 
				try {
					if(emoteS.size()>=max) {
						int last=emoteS.size()-emojiCycleNumber;
						long time=Long.MAX_VALUE;
						for(int i=emoteS.size()-emojiCycleNumber;i<emoteS.size()-1;i++) {
							Long time1=emojiLastOccurrence.get(emoteS.get(i).getName());
							if(time1==null) time1=-1L;
							if(time1<time) {
								time=time1; last=i;
							}
						}
						emoteS.get(last).delete();
						emoteS.remove(last);
					}
					server.createCustomEmojiBuilder()
						.setImage(new URL("https://emot.cf/big/"+emote+".png"))
						.setName(emote)
						.create();
					if(emoteA.size()>=max) {
						int last=0;
						long time=Long.MAX_VALUE;
						for(int i=emoteA.size()-emojiCycleNumber;i<emoteA.size()-1;i++) {
							Long time1=emojiLastOccurrence.get(emoteA.get(i).getName());
							if(time1==null||time1<time) {
								time=time1; last=i;
							}
						}
						emoteA.get(last).delete();
						emoteA.remove(last);
					}
					server.createCustomEmojiBuilder()
						.setImage(new URL("https://emot.cf/big/"+emote+".gif"))
						.setName(emote)
						.create();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    }
    
    //command methods
    private void setPrefix() {//changes the prefix
    	if(input.length<2) {//exits if no prefix was provided
    		event.getChannel().sendMessage(prefix+"setprefix [new prefix]");
    		return;
    	}
        prefix = fullInput.substring(prefix.length() + input[0].length() + 1);//changes the prefix to the new value
        event.getChannel().sendMessage("New Prefix: `" + prefix + "`");
    }
    
    private void problemList() { //responds to commands regarding user problem to-do lists
        if (input.length < 2) return; //exits if the command is incomplete
        if (input[1].equals("set")) {
            if (input.length < 4) { //exits if there are insufficient parameters
                event.getChannel().sendMessage(prefix + "problemlist set [problem link] [status]");
                return;
            }
            String problem = users.get(author).setProblem(input[2], input[3]); //sets the problem status and notifies user
            event.getChannel().sendMessage("Problem " + problem + " has been set to " + input[3] + ".");
        } else if (input[1].equals("add")) {
            if (input.length < 3) { //exits if there are insufficient parameters
                event.getChannel().sendMessage(prefix + "problemlist add [problem link 1] [problem link 2] [...]");
                return;
            }
            String output = new String();
            for (int i = 2; i < input.length; i++) { //add each problem to the user's list with the default status "to do"
                String problem = users.get(author).setProblem(input[i], "to do");
                output += "Problem " + problem + " has been added.\n";
            }
            if (output.length() > MAXLENGTH) output = output.substring(0, MAXLENGTH) + "..."; //notifies users of the added problems, truncates message if it is too long
            event.getChannel().sendMessage(output);
        } else if (input[1].equals("remove")) {
            if (input.length < 3) { //exits if there are insufficient parameters
                event.getChannel().sendMessage(prefix + "problemlist remove [problem link 1] [problem link 2] [...]");
                return;
            }
            String output = new String();
            for (int i = 2; i < input.length; i++) { //removes each problem from the user's list
                String problem = users.get(author).removeProblem(input[i]);
                if (problem == null) output += "The problem <" + input[i] + "> was not in your list.\n";
                else output += "Problem " + problem + " has been removed.\n";
            }
            if (output.length() > MAXLENGTH) output = output.substring(0, MAXLENGTH) + "..."; //notifies users of the removed problems, truncates message if it is too long
            event.getChannel().sendMessage(output);
        } else if (input[1].equals("view")) {
            long user = author; //which user's list to view, by default it is the sender of the message
            String platform = null, status = null, order = null; //filter by platform, status, or order by difficulty
            for (int i = 2; i < input.length; i++) { //parses input for filtering and ordering options
                if (input[i].startsWith("platform=")) platform = input[i].substring(9);
                else if (input[i].startsWith("status=")) status = input[i].substring(7);
                else if (input[i].startsWith("order=")) order = input[i].substring(6);
                else { //checks if the input specifies which user to view the list of
                    Long temp = parseUser(input[i], event.getServer().orElse(null));
                    if (temp == null) event.getChannel().sendMessage("User " + input[i] + " not found.");
                    else user = temp;
                }
            }
            ArrayList < UserProblem > problemList = users.get(user).getProblems(status, platform, order); //get the user's problem list
            String[] output;
            if (problemList.isEmpty()) output = new String[] { //if there are no problems in the list, output "empty"
                "Empty"
            };
            else { //otherwise output the list
                output = new String[problemList.size()];
                for (int i = 0; i < problemList.size(); i++) output[i] = problemList.get(i).toStringEmbed() + "\n";
            }
            EmbedBuilder embed = new EmbedBuilder().addField(api.getUserById(user).join().getDiscriminatedName() + "'s To Do List:", "Loading..."); //formats the output as an embed
            final long user2 = user;
            event.getChannel().sendMessage(embed).thenAccept(message -> { //allows the user to react to scroll through the list
                Scroller scroller = new Scroller(message, embed, api.getUserById(user2).join().getDiscriminatedName() + "'s To Do List:", output, 10);
                message.addReactionAddListener(scroller).removeAfter(5, TimeUnit.MINUTES); //stop watching for reactions after 5 minutes
            });
        } else if (input[1].equals("merge")) {
            if (input.length < 3) { //exits if there are insufficient parameters
                event.getChannel().sendMessage(prefix + "problemlist merge [user]");
                return;
            }
            Long user = parseUser(input[2], event.getServer().orElse(null)); //gets the user to copy from
            if (user == null) event.getChannel().sendMessage("User " + input[2] + " not found."); //checks if user is valid
            else {
                ArrayList < UserProblem > problemList = users.get(user).getProblems(null, null, null); //gets the user's problem list
                for (UserProblem cur: problemList) users.get(author).addProblem(cur); //copies the problems into the message sender's list
                event.getChannel().sendMessage("Copied " + input[2] + "'s todo list into yours.");
            }
        } else if(input[1].equals("clear")) {
        	users.get(author).clearProblems();
        	event.getChannel().sendMessage("Problem list cleared.");
        }
    }
    
    private void problemVote() { //commands regarding voting on problems
        if (input.length < 3) { //exits if there are insufficient parameters
            event.getChannel().sendMessage(prefix + "problemvote [view|like|dislike|comment] [problem link] [comment (if applicable)]");
            return;
        }
        if (!problems.containsKey(input[2])) problems.put(input[2], new GlobalProblem(input[2])); //adds the problem to problem list if it isn't there
        if (input[1].equalsIgnoreCase("like")) {
            if (problems.get(input[2]).addVote(author, 1)) //upvotes the problem if the user has not already voted
                event.getChannel().sendMessage("Liked <" + input[2] + ">! New score: " + problems.get(input[2]).getScore());
            else
                event.getChannel().sendMessage("You've already liked that problem.");
        } else if (input[1].equalsIgnoreCase("dislike")) {
            if (problems.get(input[2]).addVote(author, -1)) //downvotes the problem if the user has not already voted
                event.getChannel().sendMessage("Disliked <" + input[2] + ">! New score: " + problems.get(input[2]).getScore());
            else
                event.getChannel().sendMessage("You've already disliked that problem.");
        } else if (input[1].equalsIgnoreCase("comment")) {
            if (input.length < 4) { //exits if there are insufficient parameters
                event.getChannel().sendMessage(prefix + "problemvote comment [problem link] [comment]");
                return;
            }
            problems.get(input[2]).addComment(api.getUserById(author).join().getDiscriminatedName() + ": " + //adds the comment to the problem, along with the user's username
                fullInput.substring(prefix.length() + input[0].length() + input[1].length() + input[2].length() + 3));
            event.getChannel().sendMessage("Comment submitted!");
        } else if (input[1].equalsIgnoreCase("view")) {
            GlobalProblem cur = problems.get(input[2]); //gets the specified problem
            ArrayList < String > comments = cur.getComments(); //gets the problem's comments
            String[] output;
            if (comments.isEmpty()) output = new String[] { //if there are no comments, output "none"
                "None"
            };
            else { //otherwise output the comments
                output = new String[comments.size()];
                for (int i = 0; i < comments.size(); i++) output[i] = comments.get(i) + "\n";
            }
            EmbedBuilder embed = new EmbedBuilder().setTitle(cur.toStringEmbed()) //formats the output in a scrollable embed
                .addInlineField("Score", String.valueOf(cur.getScore()))
                .addInlineField("Difficulty", Integer.toString(cur.getDifficulty()))
                .addField("Comments", "Loading");
            event.getChannel().sendMessage(embed).thenAccept(message -> {
                Scroller scroller = new Scroller(message, embed, "Comments", output, 3);
                message.addReactionAddListener(scroller).removeAfter(5, TimeUnit.MINUTES);
            });
        }
    }
    
    private void setName(String[] input,String fullInput) { //commands regarding setting dmoj, codeforces, oichecklist accounts and real life name
    	TextChannel curChannel=event.getChannel();
    	long author=this.author;
        if (input.length < 3) { //exits if there are insufficient parameters
            curChannel.sendMessage(prefix + "setname [dmoj|cf|oichecklist|reallife] [name|remove]");
            return;
        }
        String name; //the name the user is changing to
        if (input[2].equalsIgnoreCase("remove")) name = ""; //if the name is being removed, set it to a blank string
        else name = fullInput.substring(prefix.length() + input[0].length() + input[1].length() + 2); //otherwise set it to the remainder of the string
        int success = 1;
        if (input[1].equalsIgnoreCase("reallife")) users.get(author).setRealName(name); //sets real life name
        else if (input[1].equalsIgnoreCase("oichecklist")) users.get(author).setOiCheckList(name); //sets oichecklist link
        else if (input[1].equalsIgnoreCase("dmoj")) success = users.get(author).setDmojName(name,"x"+author*3); //sets dmoj username, checks if it is valid
        else if (input[1].equalsIgnoreCase("cf")) success = users.get(author).setCfName(name,"x"+author*3); //sets codeforces username, checks if it is valid
        else { //exit if the user has not selected any of the options
            curChannel.sendMessage(prefix + "setname [dmoj|cf|oichecklist|reallife] [name|remove]");
            return;
        }
        if (name.isEmpty()) curChannel.sendMessage("Removed name for " + input[1]); //tells user the result
        else if (success==1) {
        	if(input[1].equals("dmoj")) userFromDmoj.put(name,author);
        	else if(input[1].equals("cf")) userFromCf.put(name,author);
        	curChannel.sendMessage("Set " + input[1] + " name to `" + name + "`");
        }
        else if (success==0) curChannel.sendMessage(name + " is an invalid name for " + input[1]);
        else if(success==-1) {
        	if(input[1].equals("dmoj")) curChannel.sendMessage("Put `x"+author*3 + "` in your dmoj profile description and try again.");
        	else if(input[1].equals("cf")) curChannel.sendMessage("Put `x"+author*3 + "` in your first name on codeforces and try again.");
        }
    }
    
    private void leaderboard() { //commands regarding ranking the server members in a leaderboard
        if (input.length < 2) { //exits if there are insufficient parameters
            event.getChannel().sendMessage(prefix + "leaderboard [dmojrating|cfrating|dmojmaxrating|cfmaxrating|dmojpoints|dmojpp|cfpp|problemnumber]");
            return;
        }
        ArrayList < User > leaderboard = new ArrayList < User > (users.values()); //list of users on leaderboard
        String[] output = new String[leaderboard.size()]; //the leaderboard
        String title = ""; //the title of the leaderboard
        
        if (input[1].equalsIgnoreCase("dmojrating")) {
            leaderboard.sort(Comparator.comparing(User::getDmojRating).reversed()); //sort users by descending dmoj rating and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getDmojName() + " " + leaderboard.get(i).getDmojRating() + "\n";
            title = "Top dmoj ratings";
        } else if (input[1].equalsIgnoreCase("cfrating")) {
            leaderboard.sort(Comparator.comparing(User::getcfRating).reversed()); //sort users by descending codeforces rating and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getCfName() + " " + leaderboard.get(i).getcfRating() + "\n";
            title = "Top codeforces ratings";
        } else if (input[1].equalsIgnoreCase("dmojmaxrating")) {
            leaderboard.sort(Comparator.comparing(User::getDmojMax).reversed()); //sort users by descending max dmoj rating and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getDmojName() + " " + leaderboard.get(i).getDmojMax() + "\n";
            title = "Top dmoj max ratings";
        } else if (input[1].equalsIgnoreCase("cfmaxrating")) {
            leaderboard.sort(Comparator.comparing(User::getCfMax).reversed()); //sort users by descending max codeforces rating and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getCfName() + " " + leaderboard.get(i).getCfMax() + "\n";
            title = "Top codeforces max ratings";
        } else if (input[1].equalsIgnoreCase("dmojpoints")) {
            leaderboard.sort(Comparator.comparing(User::getDmojPoints).reversed()); //sort users by descending dmoj points and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getDmojName() + " " + leaderboard.get(i).getDmojPoints() + "\n";
            title = "Top dmoj points";
        } else if (input[1].equalsIgnoreCase("dmojpp")) {
            leaderboard.sort(Comparator.comparing(User::getDmojPP).reversed()); //sort users by descending dmoj performance points and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getDmojName() + " " + leaderboard.get(i).getDmojPP() + "\n";
            title = "Top dmoj pp";
        } else if (input[1].equalsIgnoreCase("cfpp")) {
            leaderboard.sort(Comparator.comparing(User::getCfPP).reversed()); //sort users by descending codeforces performance points and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getCfName() + " " + leaderboard.get(i).getCfPP() + "\n";
            title = "Top codeforces pp";
        } else if (input[1].equalsIgnoreCase("problemnumber")) {
            leaderboard.sort(Comparator.comparing(User::getTotalPN).reversed()); //sort users by descending total problems solved and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getDmojName() + " " + leaderboard.get(i).getTotalPN() + "\n";
            title = "Most solved problems:";
        } else { //if the user has not selected any of these ranking options, exit
            event.getChannel().sendMessage(prefix + "leaderboard [dmojrating|cfrating|dmojmaxrating|cfmaxrating|dmojpoints|dmojpp|cfpp|problemnumber]");
            return;
        }
        EmbedBuilder embed = new EmbedBuilder().addField(title, "loading"); //formats the output as an embed
        final String title2 = title;
        event.getChannel().sendMessage(embed).thenAccept(message -> {
            Scroller scroller = new Scroller(message, embed, title2, output, 10);
            message.addReactionAddListener(scroller).removeAfter(5, TimeUnit.MINUTES);
        });

    }
    
    private void solvedProblems(String[] input) { //shows a user's recently solved problems
    	String user;
        if (input.length >= 2) user=input[1];
        else if(!users.get(author).getDmojName().isEmpty()) user=users.get(author).getDmojName();
        else { //exit if insufficient parameters
            event.getChannel().sendMessage(prefix + "solvedproblems [dmoj username]");
            return;
        }
        try {
        	TextChannel curChannel=event.getChannel();
            ArrayList < JSONObject > subs = new ArrayList < JSONObject > (), problems = new ArrayList < JSONObject > (); //list of user submissions, list of solved problems
            HashSet < String > solved = new HashSet < String > (); //set of the user's solved problems
            JSONObject temp;
            int page = 1;
            do {
                temp = (JSONObject) DmojCfApi.query("https://dmoj.ca/api/v2/submissions?result=ac&user=" + user + "&page=" + page).get("data"); //get each page of the user's data
                subs.addAll((ArrayList < JSONObject > ) temp.get("objects")); //add that page into the submission list
                page++; //go to next page
            } while (page<= ((Long)temp.get("total_pages")).intValue()); //while there are more pages in the data
            for (JSONObject cur: subs) //add each submission to the solved list and set if it has not been added already to the set
                if (solved.add((String) cur.get("problem"))) problems.add(cur);
            ArrayList<String> output = new ArrayList<String>();
            for (int i = problems.size() - 1; i >= 0; i--) { //go through the last 10 submissions
                Problem info = DmojCfApi.dmojProblemInfo((String) problems.get(i).get("problem")); //get info about the problem
                if (info != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    Date d1 = sdf.parse((String) problems.get(i).get("date")), d2 = new Date(); //get the time the problem was solved, and the current time
                    output.add("[" + info.getTitle() + "]("+info.getLink() + ") [" + info.getDifficulty() + "] (" + (d2.getTime() - d1.getTime()) / 86400000 + " days ago)\n"); //output problem info
                }
            }
            if (output.isEmpty()) output.add("No problems found"); //output if the user has no solved problems
            EmbedBuilder embed = new EmbedBuilder().addField("Recently solved problems by " + user, "loading"); //format the output as an embed
            curChannel.sendMessage(embed).thenAccept(message ->{
            	message.addReactionAddListener(new Scroller(message,embed,"Recently solved problems by " + user,output.toArray(new String[output.size()]),10)).removeAfter(5, TimeUnit.MINUTES);});
            	
        } catch (Exception e) { //exit if there was a problem getting the submissions
            event.getChannel().sendMessage("Error");
            System.err.println("Error finding recently solved problems by " + user);
            e.printStackTrace();
        }
    }
    private void downloadSubs(String[] input) { //shows a user's recently solved problems
    	if(!stopDownload) {
    		event.getChannel().sendMessage("Another download by "+currentDownload+" is in progress.");
    		return;
    	}
    	stopDownload=false;
    	currentDownload=author;
    	File directory=new File("submissions/");
    	if(!directory.exists()) directory.mkdir();
    	File[] files = directory.listFiles();
    	for(File f:files) f.delete();
    	TextChannel curChannel=event.getChannel(); 
        try {
        	Message message=curChannel.sendMessage("Downloading "+input[1]+" submissions for "+input[2]).join();
        	if(input[1].equals("cf")) {
        		Map<String,String> cookie=null;
        		if(input.length>3) {
            		event.getMessage().delete();
	            	Connection.Response x=Jsoup.connect("http://codeforces.com/enter").timeout(30000).method(Connection.Method.GET).execute();
	            	Document y=x.parse();
	                FormElement z=(FormElement) y.select("#enterForm").first();
	                Element zz=z.select("#handleOrEmail").first();
	                zz.attr("value",input[2]);
	                zz=z.select("#password").first();
	                zz.attr("value","input[3]");
	                z.submit().cookies(x.cookies()).execute();
	            	cookie=x.cookies();
        		}
                ArrayList < JSONObject > subs = (ArrayList < JSONObject > ) DmojCfApi.query("https://codeforces.com/api/user.status?handle=" + input[2]).get("result"),problems = new ArrayList < JSONObject > ();
                HashSet < String > solved = new HashSet < String > ();
                for (JSONObject cur: subs) {
                	String contest;
                	if(((JSONObject) cur.get("problem")).containsKey("contestId")) contest=((Long)((JSONObject) cur.get("problem")).get("contestId")).toString();
                	else if(cookie!=null) contest=((JSONObject) cur.get("problem")).get("problemsetName").toString();
                	else continue;
                	String index=(String)((JSONObject) cur.get("problem")).get("index");
                    if (cur.get("verdict").equals("OK") && (contest.length()<=5||cookie!=null) && solved.add(contest+index))
                    	problems.add(cur);
                }
            	int cnt,cnt2=0;
            	boolean[] done=new boolean[problems.size()];
            	for(int attempt=0;attempt<5;attempt++) {
                	if(stopDownload) {
                		curChannel.sendMessage("Aborted");
                		break;
                	}
            		cnt=-1;
	                for(JSONObject cur:problems) {
		            	if(++cnt%20==19) message.edit("Downloading "+input[1]+" submissions for "+input[2]+"\n"+(cnt+1)+"/"+problems.size());
	                	if(!done[cnt]){
		                	if(stopDownload) break;
		                	String contest;
		                	if(((JSONObject) cur.get("problem")).containsKey("contestId")) contest=((Long)((JSONObject) cur.get("problem")).get("contestId")).toString();
		                	else contest=((JSONObject) cur.get("problem")).get("problemsetName").toString();
		                	String index=(String)((JSONObject) cur.get("problem")).get("index");
		                	if(++cnt2%160==0) Thread.sleep(440000);
			            	String str=new String();
	                    	Connection.Response xx;
	                    	if(cookie==null) xx=Jsoup.connect("https://codeforces.com/contest/"+contest+"/submission/"+cur.get("id"))
	                    			.timeout(30000).method(Connection.Method.GET).execute();
	                    	else xx=Jsoup.connect("https://codeforces.com/contest/"+contest+"/submission/"+cur.get("id"))
	                    			.cookies(cookie).timeout(30000).method(Connection.Method.GET).execute();
			            	Document doc=xx.parse();
		                    str=doc.select(".linenums").text();
		                    if(!str.isEmpty()) {
		                    	done[cnt]=true;
				            	BufferedWriter writer = new BufferedWriter(new FileWriter(new File("submissions/"+contest+index+CFEXT.get(cur.get("programmingLanguage")))));
				                writer.write(str);
			                    writer.close();
		                    }
		                    else if(attempt==4) curChannel.sendMessage("Failed to download "+"<https://codeforces.com/contest/"+contest+"/submission/"+cur.get("id")+"> ("+contest+index+")");
		                }
	                }
                    curChannel.sendMessage("Completed attempt "+attempt);
            	}
            	if(cookie==null) curChannel.sendMessage("Gym and Acmsguru problems were not downloaded. Enter your Codeforces password as the third argument to download them.");
        	}
        	else {
        		event.getMessage().delete();
	            ArrayList < JSONObject > subs = new ArrayList < JSONObject > (), problems = new ArrayList < JSONObject > (); //list of user submissions, list of solved problems
	            HashSet < String > solved = new HashSet < String > (); //set of the user's solved problems
	            JSONObject temp;
	            int page = 1;
	            do {
	            	message.edit("Downloading "+input[1]+" submissions for "+input[2]+"\nGetting submissions page "+page);
	                temp = (JSONObject) DmojCfApi.query("https://"+input[1]+".ca/api/v2/submissions?result=ac&user=" + input[2] + "&page=" + page,"Authorization","Bearer "+input[3]).get("data"); //get each page of the user's data
	                subs.addAll((ArrayList < JSONObject > ) temp.get("objects")); //add that page into the submission list
	                page++; //go to next page
	            } while (page<= ((Long)temp.get("total_pages")).intValue()); //while there are more pages in the data
	            Collections.reverse(subs);
	            for (JSONObject cur: subs) 
	                if (solved.add((String) cur.get("problem"))) problems.add(cur);
	            int cnt=0;
	            for (JSONObject cur:problems) { 
	            	if(stopDownload) {
                		curChannel.sendMessage("Aborted");
                		break;
	            	}
	            	if(++cnt%5==0) message.edit("Downloading "+input[1]+" submissions for "+input[2]+"\n"+cnt+"/"+problems.size());
	                String text=DmojCfApi.dmojSubmission("https://"+input[1]+".ca/src/"+ cur.get("id")+"/raw","Authorization","Bearer "+input[3]);
	                BufferedWriter writer = new BufferedWriter(new FileWriter(new File("submissions/"+cur.get("problem")+DMOJEXT.get(cur.get("language")))));
	                writer.write(text);
	                writer.close();
	            }
        	}
            message.edit("Downloading "+input[1]+" submissions for "+input[2]+"\nCompleted.");
            files = directory.listFiles();FileOutputStream fos = new FileOutputStream("compressed.zip");
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            for (File file : files) {
                FileInputStream fis = new FileInputStream(file);
                ZipEntry zipEntry = new ZipEntry(file.getName());
                zipOut.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                fis.close();
                file.delete();
            }
            zipOut.close();
            fos.close();
            File zip=new File("compressed.zip");
            curChannel.sendMessage(zip).thenAccept(awedkluqhaweifzzz ->{zip.delete();});
        } catch (Exception e) { //exit if there was a problem getting the submissions
        	curChannel.sendMessage("Error");
            System.err.println("Error");
            e.printStackTrace();
        }
        stopDownload=true;
    }
    
    private void recommend() { //recommends new problems for the user
        int minDif = 0, maxDif = 10000; //minDif: minimum difficulty, maxDif: maximum difficulty for the recommended problem
        ArrayList < String > tags = new ArrayList < String > (); //list of tags the problem must have
        boolean includeSolved = false; //whether previously solved problems can be recommended again
        String search = "", output = ""; //search: a regex string to filter problem codes
        for (int i = 1; i < input.length; i++) {
            if (input[i].startsWith("min=")) { //set min difficulty
                try {
                    minDif = Integer.parseInt(input[i].substring(4));
                } catch (NumberFormatException e) {} //skip this parameter if it is not a valid integer
            } else if (input[i].startsWith("max=")) { //set max difficulty
                try {
                    maxDif = Integer.parseInt(input[i].substring(4));
                } catch (NumberFormatException e) {} //skip if this parameter is not a valid integer
            } else if (input[i].equalsIgnoreCase("includeSolved")) includeSolved = true; //set includeSolved search parameters
            else if (input[i].startsWith("+")) tags.add(input[i].substring(1).toLowerCase()); //set search tags
            else if (input[i].startsWith("search=")) search = input[i].substring(7); //set regex search filter
            else if (input[i].equalsIgnoreCase("beginner")) { //set min and max difficulty based on inputed skill level
                minDif = 3;
                maxDif = 5;
            } else if (input[i].equalsIgnoreCase("intermediate")) {
                minDif = 7;
                maxDif = 12;
            } else if (input[i].equalsIgnoreCase("advanced")) {
                minDif = 12;
                maxDif = 20;
            } else if (input[i].equalsIgnoreCase("expert")) {
                minDif = 17;
                maxDif = 25;
            } else if (input[i].equalsIgnoreCase("extreme")) {
                minDif = 25;
                maxDif = 50;
            }
        }
        HashSet < String > solved = new HashSet < String > (); //set of problems the user already solved
        try {
            if (!includeSolved && !users.get(author).getDmojName().isEmpty()) { //if the user is linked to dmoj, and does want solved problems, add all their solved problems to the set
                ArrayList < String > userSubmissions = (ArrayList < String > )((JSONObject)((JSONObject) DmojCfApi.query("https://dmoj.ca/api/v2/user/" + users.get(author).getDmojName()).get("data")).get("object")).get("solved_problems");
                for (String cur: userSubmissions) solved.add(cur);
            }
        } catch (IOException | ParseException | InterruptedException e) { //skip this search setting if an error occurs
            System.err.println("Error getting solved problems by user");
            e.printStackTrace();
        }
        search = ".*" + search + ".*"; //this allows the search string to match any substring of the problem code
        ArrayList < Problem > problems = DmojCfApi.getDmojProblems(); //get list of dmoj problems from cache
        Collections.shuffle(problems); //shuffle the list to get random problems recommended each time
        int problemNumber = 0; //number of valid recommendations found so far
        for (Problem cur: problems) { //iterate through all dmoj problems
            if (minDif <= cur.getDifficulty() && cur.getDifficulty() <= maxDif && //if difficulty is within the search range
                !solved.contains(cur.getCode()) && //and it is not in the solved problems set
                (search.isEmpty() || cur.getCode().matches(search))) { //and it matches the search string, if the user provided one
                boolean valid = true; //whether the problem is valid
                for (String tag: tags) //for each search tag, the problem must have that tag to be calid
                    if (!cur.containsTag(tag)) valid = false;
                if (valid) { //if the problem is valid, add it to the recommendation list
                    output += "[" + cur.getTitle() + "]("+cur.getLink() + ") [" + cur.getDifficulty() + "]\n";
                    if (++problemNumber >= 10) break; //if 10 problems have been recommended, exit
                }
            }
        }
        if (output.isEmpty()) output = ":star2: You solved all problems of this type!"; //output if there are no more recommendations for the user
        EmbedBuilder embed = new EmbedBuilder().addField("Recommended problems", output); //otherwise output the recommendations as an embed
        event.getChannel().sendMessage(embed);
    }
    private void spam(String[] input,String fullInput) {
		if(input.length<3) return;
		int numb=Integer.parseInt(input[1]);
		String output=fullInput.substring(prefix.length()+input[0].length()+input[1].length()+2);
		for(int i=0;i<numb;i++) event.getChannel().sendMessage(output);
    }
    
    //main message responder
    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        author = event.getMessageAuthor().getId(); //get the sender of the message
        if (author == api.getClientId()) return; //check if the message was sent by the bot itself, and exit so that it doesn't reply to itself
        this.event=event;
        fullInput=event.getMessageContent();
        
        if(emojiCycleNumber>0) event.getServer().ifPresent(server -> {if(server.getId()==HOME) updateEmotes(server);});

        if (!fullInput.startsWith(prefix)) return;
        prevTime=new Date().getTime();
        input = fullInput.substring(prefix.length()).split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)"); //split the message based on spaces, but ignore spaces in quotes
        for (int i = 0; i < input.length; i++) input[i] = input[i].replace("\"", ""); //remove quotes from the input        
        //bot owner commands
        if(author==api.getOwnerId()||author==AAERIA) switch(input[0].toLowerCase()) {
        case "getservers":
        	Collection<Server> servers= api.getServers();
        	event.getChannel().sendMessage(Integer.toString(servers.size()));
        	for(Server cur:servers) event.getChannel().sendMessage(cur.getName()+" "+cur.getId());
        	break;
        case "leaveserver":
        	api.getServerById(input[1]).ifPresent(server->{server.leave(); event.getChannel().sendMessage("Left "+server.getName());});
        	break;
        case "sethome":
        	HOME=Long.parseLong(input[1]);
        	event.getChannel().sendMessage("Set home server to "+input[1]);
        	break;
        case "adminrole":
        	if(adminRole.contains(input[1])) {
        		adminRole.remove(input[1]);
        		event.getChannel().sendMessage("Removed `"+input[1]+"` as an admin role.");
        	}
        	else {
        		adminRole.add(input[1]);
        		event.getChannel().sendMessage("Added `"+input[1]+"` as an admin role.");
        	}
        	break;
        }
        //admin
        boolean isAdmin=false;
        if(api.getServerById(HOME).isPresent()) {
	        List<Role> roles=api.getUserById(author).join().getRoles(api.getServerById(HOME).get());
	        for(Role cur:roles) if(adminRole.contains(cur.getName())) isAdmin=true;
        }
        if(author==api.getOwnerId()||author==AAERIA||isAdmin) switch(input[0].toLowerCase()) {
        case "setprefix":
        	setPrefix();
        	break;
        case "sudo":
        	if(input.length<2) break;
        	Long user=parseUser(input[1],event.getServer().orElse(null));
        	if(user==null) {
        		event.getChannel().sendMessage(input[1]+" not found.");
        		break;
        	}
        	author=user;
        	fullInput=prefix+fullInput.substring(prefix.length()+input[0].length()+input[1].length()+2);
        	input=Arrays.copyOfRange(input, 2, input.length);
        	break;
        case "spam":
        	new Thread(() -> {
        		spam(input,fullInput);
    		}).start(); 
        	break;
        case "emotecycle":
        	if(emojiCycleNumber==0) {
        		emojiCycleNumber=10;
        		event.getChannel().sendMessage("Cycling on.");
        	}
        	else {
        		emojiCycleNumber=0;
        		event.getChannel().sendMessage("Cycling off.");
        	}
        	break;
        case "set":
        	input[3]=input[3].toLowerCase();
        	switch(input[1].toLowerCase()) {
        	case "dmoj":
        		if(users.get(parseUser(input[2],event.getServer().orElse(null))).setDmojName(input[3], "")==1) {
        			userFromDmoj.put(input[3],parseUser(input[2],event.getServer().orElse(null)));
        			event.getChannel().sendMessage("success");
        		}
        		else event.getChannel().sendMessage("error");
        		break;
        	case "cf":
        		if(users.get(parseUser(input[2],event.getServer().orElse(null))).setCfName(input[3], "")==1) {
        			userFromCf.put(input[3],parseUser(input[2],event.getServer().orElse(null)));
        			event.getChannel().sendMessage("success");
        		}
        		else event.getChannel().sendMessage("error");
        		break;
        	case "problemscore":
        		problems.get(input[2]).setScore(Integer.parseInt(input[3]));
        		event.getChannel().sendMessage("done");
        		break;
        	}
        	break;
        case "deletecomment":
        	problems.get(input[2]).deleteComment(Integer.parseInt(input[3]));
    		event.getChannel().sendMessage("done");
    		break;
        case "abort":
        	stopDownload=true;
        	break;
        }
        if (!users.containsKey(author)) users.put(author, new User(author)); //if the author is not in the bot's user name, add him/her
        
        //call the apropriate method for each command:
        switch(input[0].toLowerCase()) {
        case "ping":
        	event.getChannel().sendMessage("Pong!");
        	break;
        case "help"://sends an embed with bot information
            EmbedBuilder embed = new EmbedBuilder()
            .setAuthor(api.getUserById(api.getClientId()).join().getName(), null, api.getUserById(api.getClientId()).join().getAvatar())
            .addField("Documentation", "[Documentation site](https://docs.xadelaide.cf/)")
            .addField("Commands", "[Command List](https://docs.xadelaide.cf/commands-1/)")
            .addField("Invite", "[Invite link](" + api.createBotInvite() + PERMISSIONS + ")")
        	.addField("Support", "[Server link](http://cccdiscord.cf/)");
            event.getChannel().sendMessage(embed);
            break;
        case "problemlist":
        	problemList();
        	break;
        case "problemvote":
        	problemVote();
        	break;
        case "id":
        case "setname":
        	 new Thread(() -> {
                 setName(input,fullInput);
             }).start(); //execute this command on a separate thread so that it can run in the background while not affecting other users
        	break;
        case "ui":
        case "userinfo":
            Long user = author;
            if (input.length > 1) user = parseUser(input[1], event.getServer().orElse(null));
            if (!users.containsKey(user)) event.getChannel().sendMessage("User " + input[1] + " not found.");
            else event.getChannel().sendMessage(users.get(user).showInfo().setTitle(api.getUserById(user).join().getDiscriminatedName()).setThumbnail(api.getUserById(user).join().getAvatar()));
        	break;
        case "users":
        case "leaderboard":
        	leaderboard();
        	break;
        case "sp":
        case "solvedproblems":
        	new Thread(() -> {
        		solvedProblems(input);
        	}).start(); //execute this command on a separate thread
        	break;
        case "recommend":
        	recommend();
        	break;
        case "getuserbyid":
        	if(input.length<2) break;
        	event.getChannel().sendMessage(api.getUserById(input[1]).join().getDiscriminatedName());
        	break;
        case "downloadsubs":
	       	new Thread(() -> {
	       		downloadSubs(input);
	       	}).start();
        	break;
        case "contest":
        	try {
        		if(input.length<2) {
        			event.getChannel().sendMessage(prefix+"contest [contestid] (forcerate) (showall)");
        			break;
        		}
				ArrayList<JSONObject> temp2=(ArrayList<JSONObject>) ((JSONObject) ((JSONObject) DmojCfApi.query("https://dmoj.ca/api/v2/contest/"+input[1],"Authorization","Bearer AAA3gX2Rr8Bc55Wh3V_3UtktA218NZYRhpTRu7D31_0nJ3-m").get("data")).get("object")).get("rankings");
        		int n=temp2.size()+1,cnt=0;
				int[] old=new int[n],vol=new int[n],perf=new int[n+1],change=new int[n+1];
				boolean rated=false,forcerate=false,showall=false;
				for(int i=0;i<input.length;i++) {
					if(input[i].equalsIgnoreCase("forcerate")) forcerate=true;
					else if(input[i].equalsIgnoreCase("showall")) showall=true;
				}
				try {
					Map<String,JSONObject> temp=(Map<String, JSONObject>) DmojCfApi.query("https://evanzhang.ca/rating/contest/"+input[1]+"/api").get("users");
					rated=!temp.isEmpty();
					for(JSONObject cur:temp.values()) {
						cnt=((Double)cur.get("rank")).intValue();
						while(vol[cnt]!=0) cnt++;
						if(cur.get("old_rating")==null||cur.get("rating_change")==null) old[cnt]=vol[cnt]=-1000;
						else {
							old[cnt]=((Long) cur.get("old_rating")).intValue();
							vol[cnt]=((Long) cur.get("volatility")).intValue();
						}
						if(cur.get("rating_change")!=null)
							change[cnt]=((Long)cur.get("rating_change")).intValue();
					}
					for(int i=1;i<n;i++) if(vol[i]==0) old[i]=vol[i]=-1000;
					double a;
					for(int rating=-999;rating<=5000;rating++) {
						a=0;
						for(int i=1;i<n;i++) if(old[i]!=-1000) a+=erf((old[i]-rating)/Math.sqrt(2)/vol[i]);
						perf[(int) Math.floor(a+0.5)]=rating;
						if(rating==-999) cnt=(int) Math.floor(a+0.5);
					}
					//for(int i=0;i<=n;i++) System.out.println(i+" "+perf[i]);
					int b=-999;
					for(int i=n-1;i>0;i--) {
						if(old[i]==-1000) perf[i]=b; 
						else perf[i]=b=perf[cnt--];
					}
					//for(int i=0;i<=n;i++) System.out.println(i+" "+perf[i]);
				} catch(Exception e) {
					e.printStackTrace();
					rated=false;
				}
				if(!rated&&forcerate) {
					rated=true;
					cnt=1;
					ArrayList<Integer> prevRatings=new ArrayList<Integer>();
					for(JSONObject cur:temp2) {
						if(cur.get("old_rating")==null) old[cnt]=-1000;
						else {
							old[cnt]=((Long)cur.get("old_rating")).intValue();
							prevRatings.add(old[cnt]);
						}
						cnt++;
					}
					ArrayList<Integer> deltas=CodeforcesRatingCalculator.calculateRatingChanges(prevRatings);
					cnt=0;
					int prev=n-1; cnt=deltas.size()-1;
					for(int i=n-1;i>=0;i--) {
						if(i==0) {
							old[i]=old[prev];
							perf[i]=perf[prev];
						}
						if(old[i]!=-1000) {
							if(i>0) {
								change[i]=deltas.get(cnt);
								perf[i]=old[i]+deltas.get(cnt)*4;
							}
							for(int j=i+1;j<prev;j++) {
								perf[j]=(perf[i]*(prev-j)+perf[prev]*(j-i))/(prev-i);
								change[j]=perf[j]-1200;
							}
							cnt--; prev=i;
						}
					}
				}
				HashSet<String> serverNames=new HashSet<String>();
				for(User cur:users.values()) serverNames.add(cur.getDmojName());
				String output="\n";
				ArrayList<String> output2=new ArrayList<String>();
				int pnumb=0,nameLen=1;
				for(JSONObject cur:temp2)
					if(showall||serverNames.contains(((String)cur.get("user")).toLowerCase()))
						nameLen=Math.max(nameLen,((String)cur.get("user")).length());
				cnt=1;
				int cnt2=0;
				for(JSONObject cur:temp2) {
					if(showall||serverNames.contains(((String)cur.get("user")).toLowerCase())){
						String s=String.format("%-4s",cnt)+String.format("%-"+nameLen+"s",(String)cur.get("user"))+" "+String.format("%-5s",Math.round((Double)cur.get("score")));
						for(JSONObject i:(ArrayList<JSONObject>)cur.get("solutions")) {
							if(i==null) s+=String.format("%-4s","-");
							else s+=String.format("%-4s",(Math.round((Double)i.get("points"))));
							if(pnumb<=0) pnumb--;
						}
						if(rated) {
							s+=String.format("%-5s",change[cnt])+String.format("%-16s",perf[cnt]+"("+perf[cnt+1]+"-"+perf[cnt-1]+")");
							if(old[cnt]==-1000) s+="N/A"+"->"+(1200+change[cnt]);
							else s+=old[cnt]+"->"+(old[cnt]+change[cnt]);
						}
						output+=s+"\n";
						if(pnumb<0) pnumb=-pnumb;
						cnt2++;
					}
					cnt++;
					if(output.length()>1800||cnt2>=30) {
						cnt2=0;
						if(rated) output="\u0394    Perf            Rating"+output;
						for(int i=0;i<pnumb;i++) output="    "+output;
						output="Rank "+String.format("%-"+nameLen+"s","Name")+"Score"+output;
						output2.add("```"+output+"```");
						output="\n";
					}
				}
				if(rated) output="\u0394    Perf            Rating"+output;
				for(int i=0;i<pnumb;i++) output="    "+output;
				output="Rank "+String.format("%-"+nameLen+"s","Name")+"Score"+output;
				output2.add("```"+output+"```");
	            event.getChannel().sendMessage("loading").thenAccept(message -> { //allows the user to react to scroll through the list
	                Scroller scroller = new Scroller(message, output2.toArray(new String[output2.size()]), 1);
	                message.addReactionAddListener(scroller).removeAfter(5, TimeUnit.MINUTES); //stop watching for reactions after 5 minutes
	            });
			} catch (IOException | ParseException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        //Date endTime=new Date();
        //System.out.println(endTime.getTime()-startTime.getTime());
    }
    double erf(double x) {
    	double ans,a; ans=a=x;
    	for(int i=1;i<29;i++) {
    		a*=-x*x/i;
    		ans+=a/(2*i+1);
    	}
    	return Math.min(Math.max(ans/Math.sqrt(Math.PI)+0.5,0),1);
    }
}