package bot;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class BotListener implements MessageCreateListener { //this class receives messages and responds to commands from users
    private final int MAXLENGTH = 1990; //max message length on discord
    private final int PERMISSIONS = 85056; //the required discord permissions for the bot
    private String prefix = "!"; //all bot commands start with this prefix
    private HashMap < Long, User > users = new HashMap < Long, User > (); //list of users using the bot
    private HashMap < String, GlobalProblem > problems = new HashMap < String, GlobalProblem > (); //list of problems that have been voted on
    private DiscordApi api; //discord api connection	
    
    public void save() { //saves important info stored by the bot
        FileOutputStream f;
        try {
            f = new FileOutputStream(new File("data.txt")); //opens data storage file, or creates one if it doesn't exist
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(users); //saves list of users
            o.writeObject(problems); //saves list of problems with votes
            o.close();
            f.close();
        } catch (IOException e) {
            System.err.println("Unable to save data to file");
            e.printStackTrace();
        }
    }
    
    public BotListener(DiscordApi api) { //BotListener constructor
        this.api = api;
        try { //retrieves data from the last time the bot was running
            FileInputStream fi;
            fi = new FileInputStream(new File("data.txt")); //gets data from storage file
            ObjectInputStream oi = new ObjectInputStream(fi);
            users = (HashMap < Long, User > ) oi.readObject(); //retrieves list of users
            problems = (HashMap < String, GlobalProblem > ) oi.readObject(); //retrieves list of problems
            System.out.println("data restored");
            oi.close();
            fi.close();
        } catch (IOException | ClassNotFoundException e) { //if the storage file does not yet exist, or cannot be read
            System.err.println("Unable to retrieve stored data file");
            e.printStackTrace();
        }
    }
    
    private Long parseUser(String input, Server s) { //recieves an input string and the bot's current server, converts input into a discord user id
        if (api.getCachedUserById(input.replaceAll("[<>@!]", "")).isPresent()) //checks if the input could be an id number or mention (eg. <@543096067059744800>)
            return api.getCachedUserById(input.replaceAll("[<>@!]", "")).get().getId();
        if (input.contains("#") && api.getCachedUserByDiscriminatedNameIgnoreCase(input).isPresent()) //checks if the input could be a username with discriminator (eg. aaeria#0001)
            return api.getCachedUserByDiscriminatedNameIgnoreCase(input).get().getId();
        if (!api.getCachedUsersByNameIgnoreCase(input).isEmpty()) //checks if the input could be a username without discriminator (eg. aaeria)
            return api.getCachedUsersByNameIgnoreCase(input).iterator().next().getId();
        if (s != null && !api.getCachedUsersByNicknameIgnoreCase(input, s).isEmpty()) //checks if the input could be the nickname of a user in the server (eg. bob)
            return api.getCachedUsersByNicknameIgnoreCase(input, s).iterator().next().getId();
        return null; //if the input is none of these, it doesn't contain a valid discord user
    }
    
    //command methods
    public void setPrefix(MessageCreateEvent event, String[] input, long author) {//changes the prefix
    	if(input.length<2) {//exits if no prefix was provided
    		event.getChannel().sendMessage(prefix+"setprefix [new prefix]");
    		return;
    	}
        prefix = event.getMessageContent().substring(prefix.length() + input[0].length() + 1);//changes the prefix to the new value
        event.getChannel().sendMessage("New Prefix: `" + prefix + "`");
    }
    
    public void problemList(MessageCreateEvent event, String[] input, long author) { //responds to commands regarding user problem to-do lists
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
            if (!users.containsKey(user)) users.put(user, new User(user)); //if the user is not in the bot's user list, add it
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
                EmbedScroller scroller = new EmbedScroller(message, embed, api.getUserById(user2).join().getDiscriminatedName() + "'s To Do List:", output, 10);
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
        }
    }
    
    public void problemVote(MessageCreateEvent event, String[] input, long author) { //commands regarding voting on problems
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
            problems.get(input[2]).addComment(event.getMessageAuthor().getDiscriminatedName() + ": " + //adds the comment to the problem, along with the user's username
                event.getMessageContent().substring(prefix.length() + input[0].length() + input[1].length() + input[2].length() + 3));
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
                EmbedScroller scroller = new EmbedScroller(message, embed, "Comments", output, 3);
                message.addReactionAddListener(scroller).removeAfter(5, TimeUnit.MINUTES);
            });
        }
    }
    
    public void setName(MessageCreateEvent event, String[] input, long author) { //commands regarding setting dmoj, codeforces, oichecklist accounts and real life name
        if (input.length < 3) { //exits if there are insufficient parameters
            event.getChannel().sendMessage(prefix + "setname [dmoj|codeforces|oichecklist|reallife] [name|remove]");
            return;
        }
        String name; //the name the user is changing to
        if (input[2].equalsIgnoreCase("remove")) name = ""; //if the name is being removed, set it to a blank string
        else name = event.getMessageContent().substring(prefix.length() + input[0].length() + input[1].length() + 2); //otherwise set it to the remainder of the string
        boolean success = true;
        if (input[1].equalsIgnoreCase("reallife")) users.get(author).setRealName(name); //sets real life name
        else if (input[1].equalsIgnoreCase("oichecklist")) users.get(author).setOiCheckList(name); //sets oichecklist link
        else if (input[1].equalsIgnoreCase("dmoj")) success = users.get(author).setDmojName(name); //sets dmoj username, checks if it is valid
        else if (input[1].equalsIgnoreCase("codeforces")) success = users.get(author).setCfName(name); //sets codeforces username, checks if it is valid
        else { //exit if the user has not selected any of the options
            event.getChannel().sendMessage(prefix + "setname [dmoj|codeforces|oichecklist|reallife] [name|remove]");
            return;
        }
        if (name.isEmpty()) event.getChannel().sendMessage("Removed name for " + input[1]); //tells user the result
        else if (success) event.getChannel().sendMessage("Set " + input[1] + " name to `" + name + "`");
        else event.getChannel().sendMessage(name + " is an invalid name for " + input[1]);
    }
    
    public void leaderboard(MessageCreateEvent event, String[] input, long author) { //commands regarding ranking the server members in a leaderboard
        if (input.length < 2) { //exits if there are insufficient parameters
            event.getChannel().sendMessage(prefix + "leaderboard [dmojrating|codeforcesrating|dmojmaxrating|codeforcesmaxrating|dmojpoints|dmojpp|codeforcespp|problemnumber]");
            return;
        }
        ArrayList < User > leaderboard = new ArrayList < User > (users.values()); //list of users on leaderboard
        String[] output = new String[leaderboard.size()]; //the leaderboard
        String title = ""; //the title of the leaderboard
        if (input[1].equalsIgnoreCase("dmojrating")) {
            leaderboard.sort(Comparator.comparing(User::getDmojRating).reversed()); //sort users by descending dmoj rating and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getDmojName() + " " + leaderboard.get(i).getDmojRating() + "\n";
            title = "Top dmoj ratings";
        } else if (input[1].equalsIgnoreCase("codeforcesrating")) {
            leaderboard.sort(Comparator.comparing(User::getcfRating).reversed()); //sort users by descending codeforces rating and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getCfName() + " " + leaderboard.get(i).getcfRating() + "\n";
            title = "Top codeforces ratings";
        } else if (input[1].equalsIgnoreCase("dmojmaxrating")) {
            leaderboard.sort(Comparator.comparing(User::getDmojMax).reversed()); //sort users by descending max dmoj rating and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getDmojName() + " " + leaderboard.get(i).getDmojMax() + "\n";
            title = "Top dmoj max ratings";
        } else if (input[1].equalsIgnoreCase("codeforcesmaxrating")) {
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
        } else if (input[1].equalsIgnoreCase("codeforcespp")) {
            leaderboard.sort(Comparator.comparing(User::getCfPP).reversed()); //sort users by descending codeforces performance points and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getCfName() + " " + leaderboard.get(i).getCfPP() + "\n";
            title = "Top codeforces pp";
        } else if (input[1].equalsIgnoreCase("problemnumber")) {
            leaderboard.sort(Comparator.comparing(User::getTotalPN).reversed()); //sort users by descending total problems solved and add to leaderboard
            for (int i = 0; i < leaderboard.size(); i++) output[i] = i + 1 + " " + leaderboard.get(i).getDmojName() + " " + leaderboard.get(i).getTotalPN() + "\n";
            title = "Most solved problems:";
        } else { //if the user has not selected any of these ranking options, exit
            event.getChannel().sendMessage(prefix + "leaderboard [dmojrating|codeforcesrating|dmojmaxrating|codeforcesmaxrating|dmojpoints|dmojpp|codeforcespp|problemnumber]");
            return;
        }
        EmbedBuilder embed = new EmbedBuilder().addField(title, "loading"); //formats the output as an embed
        final String title2 = title;
        event.getChannel().sendMessage(embed).thenAccept(message -> {
            EmbedScroller scroller = new EmbedScroller(message, embed, title2, output, 10);
            message.addReactionAddListener(scroller).removeAfter(5, TimeUnit.MINUTES);
        });

    }
    
    public void solvedProblems(MessageCreateEvent event, String[] input, long author) { //shows a user's recently solved problems
        if (input.length < 2) { //exit if insufficient parameters
            event.getChannel().sendMessage(prefix + "solvedproblems [dmoj username]");
            return;
        }
        try {
            ArrayList < JSONObject > subs = new ArrayList < JSONObject > (), problems = new ArrayList < JSONObject > (); //list of user submissions, list of solved problems
            HashSet < String > solved = new HashSet < String > (); //set of the user's solved problems
            JSONObject temp;
            int page = 1;
            do {
                temp = (JSONObject) DmojCfApi.query("https://dmoj.ca/api/v2/submissions?result=ac&user=" + input[1] + "&page=" + page).get("data"); //get each page of the user's data
                subs.addAll((ArrayList < JSONObject > ) temp.get("objects")); //add that page into the submission list
                page++; //go to next page
            } while ((boolean) temp.get("has_more")); //while there are more pages in the data
            for (JSONObject cur: subs) //add each submission to the solved list and set if it has not been added already to the set
                if (solved.add((String) cur.get("problem"))) problems.add(cur);
            String output = "";
            for (int i = problems.size() - 1; i >= Math.max(0, problems.size() - 10); i--) { //go through the last 10 submissions
                JSONObject info = DmojCfApi.dmojProblemInfo((String) problems.get(i).get("problem")); //get info about the problem
                if (info != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    Date d1 = sdf.parse((String) problems.get(i).get("date")), d2 = new Date(); //get the time the problem was solved, and the current time
                    output += "[" + info.get("name") + "](https://dmoj.ca/problem/" + problems.get(i).get("problem") + ") [" + info.get("points") + "] (" + (d2.getTime() - d1.getTime()) / 86400000 + " days ago)\n"; //output problem info
                }
            }
            if (output.isEmpty()) output = "No problems found"; //output if the user has no solved problems
            EmbedBuilder embed = new EmbedBuilder().addField("Recently solved problems by " + input[1], output); //format the output as an embed
            event.getChannel().sendMessage(embed);
        } catch (Exception e) { //exit if there was a problem getting the submissions
            event.getChannel().sendMessage("Error");
            System.err.println("Error finding recently solved problems by " + input[1]);
            e.printStackTrace();
        }
    }
    
    public void recommend(MessageCreateEvent event, String[] input, long author) { //recommends new problems for the user
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
        ArrayList < JSONObject > problems = DmojCfApi.getDmojProblems(); //get list of dmoj problems from cache
        Collections.shuffle(problems); //shuffle the list to get random problems recommended each time
        int problemNumber = 0; //number of valid recommendations found so far
        for (JSONObject cur: problems) { //iterate through all dmoj problems
            int points = ((Double) cur.get("points")).intValue(); //get difficulty point value of the problem
            String code = (String) cur.get("code"); //get problem code
            if (minDif <= points && points <= maxDif && //if difficulty is within the search range
                !solved.contains(code) && //and it is not in the solved problems set
                (search.isEmpty() || code.matches(search))) { //and it matches the search string, if the user provided one
                HashSet < String > types = new HashSet < String > (); //a set of the tags of the current problem
                boolean valid = true; //whether the problem is valid
                for (String tag: (ArrayList < String > ) cur.get("types")) types.add(tag.toLowerCase()); //add all the problem's tags to the set
                for (String tag: tags) //for each search tag, the problem must have that tag to be calid
                    if (!types.contains(tag)) valid = false;
                if (valid) { //if the problem is valid, add it to the recommendation list
                    output += "[" + cur.get("name") + "](https://dmoj.ca/problem/" + cur.get("code") + ") [" + cur.get("points") + "]\n";
                    if (++problemNumber >= 10) break; //if 10 problems have been recommended, exit
                }
            }
        }
        if (output.isEmpty()) output = ":star2: You solved all problems of this type!"; //output if there are no more recommendations for the user
        EmbedBuilder embed = new EmbedBuilder().addField("Recommended problems", output); //otherwise output the recommendations as an embed
        event.getChannel().sendMessage(embed);
    }
    
    //main message responder
    public void onMessageCreate(MessageCreateEvent event) {
        if (!event.getMessageContent().startsWith(prefix)) return; //if the message does not start with the prefix, it is not a bot command, so exit
        long author = event.getMessageAuthor().getId(); //get the sender of the message
        if (author == api.getClientId()) return; //check if the message was sent by the bot itself, and exit so that it doesn't reply to itself
        
        String[] input = event.getMessageContent().substring(prefix.length()).split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)"); //split the message based on spaces, but ignore spaces in quotes
        for (int i = 0; i < input.length; i++) input[i] = input[i].replace("\"", ""); //remove quotes from the input
        
        if (!users.containsKey(author)) users.put(author, new User(author)); //if the author is not in the bot's user name, add him/her
        
        //call the apropriate method for each command:
        if (input[0].equalsIgnoreCase("ping")) {
            event.getChannel().sendMessage("Pong!");
        } else if (input[0].equalsIgnoreCase("help")) {//sends an embed with bot information
            EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(api.getUserById(api.getClientId()).join().getName(), null, api.getUserById(api.getClientId()).join().getAvatar())
                .addField("Documentation", "[Documentation site](https://docs.xadelaide.cf/)")
                .addField("Commands", "[Command List](https://docs.xadelaide.cf/commands-1/documentation-format)")
                .addField("Invite", "[Invite link](" + api.createBotInvite() + PERMISSIONS + ")")
            	.addField("Support", "[Server link](https://discord.gg/jnJjbGw)");
            event.getChannel().sendMessage(embed);
        } else if (input[0].equalsIgnoreCase("setprefix")) setPrefix(event,input,author);
        else if (input[0].equalsIgnoreCase("problemlist")) problemList(event, input, author);
        else if (input[0].equalsIgnoreCase("problemvote")) problemVote(event, input, author);
        else if (input[0].equalsIgnoreCase("setname")) new Thread(() -> {
            setName(event, input, author);
        }).start(); //execute this command on a separate thread so that it can run in the background while not affecting other users
        else if (input[0].equalsIgnoreCase("userinfo")) {
            Long user = author;
            if (input.length > 1) user = parseUser(input[1], event.getServer().orElse(null));
            if (!users.containsKey(user)) event.getChannel().sendMessage("User " + input[1] + " not found.");
            else event.getChannel().sendMessage(users.get(user).showInfo().setTitle(api.getUserById(user).join().getDiscriminatedName()));
        } else if (input[0].equalsIgnoreCase("leaderboard")) leaderboard(event, input, author);
        else if (input[0].equalsIgnoreCase("solvedproblems")) new Thread(() -> {
            solvedProblems(event, input, author);
        }).start(); //execute this command on a separate thread
        else if (input[0].equalsIgnoreCase("recommend")) recommend(event, input, author);
    }
}