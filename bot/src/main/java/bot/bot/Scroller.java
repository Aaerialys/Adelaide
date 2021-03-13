package bot;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;

public class Scroller implements ReactionAddListener { //a message embedded with a list of many items that the user can scroll through
    private static final String PREV = "\u2b05", NEXT = "\u27A1",PREV2="\u23EE",NEXT2="\u23ED"; //unicode values for the left and right arrow emoji
    private boolean isEmbed;
    private EmbedBuilder embed; //the embed to display
    private String listTitle; //the title of the list
    private String[] scrollList; //the contents of the list
    private Message message; //the message to put the embed in
    private int page, outputMax, pageMax; //page: current page number, outputMax: max list items per page, pageMax: total page number of the list
    
    private void display() { //displays a page of the list
        String output = new String();
        for (int i = page * outputMax; i < Math.min((page + 1) * outputMax, scrollList.length); i++) output += scrollList[i]; //adds all the list items on the current page to the output
        final String output2 = output;
        if(isEmbed)
        	message.edit(embed.setFooter("Page " + (page + 1) + "/" + pageMax) //edits the message to contain the output
	            .updateFields(field -> {
	                return field.getName().equals(listTitle);
	            }, field -> {
	                field.setValue(output2);
	            }));
        else 
        	message.edit(output2);
    }
    
    public Scroller(Message message, EmbedBuilder embed, String listTitle, String[] scrollList, int outputMax) { //constructor
    	isEmbed=true;
        message.addReaction(PREV2);
        message.addReaction(PREV);
        message.addReaction(NEXT);
        message.addReaction(NEXT2);
        //initializes the instance variables
        this.message = message;
        this.embed = embed;
        this.listTitle = listTitle;
        this.scrollList = scrollList;
        page = 0;
        this.outputMax = outputMax;
        this.pageMax = (scrollList.length + outputMax - 1) / outputMax; //calculates the max page number
        display(); //displays the list
    }
    public Scroller(Message message, String[] scrollList, int outputMax) { //constructor
    	isEmbed=false;
        message.addReaction(PREV2);
        message.addReaction(PREV);
        message.addReaction(NEXT);
        message.addReaction(NEXT2);
        //initializes the instance variables
        this.message = message;
        this.scrollList = scrollList;
        page = 0;
        this.outputMax = outputMax;
        this.pageMax = (scrollList.length + outputMax - 1) / outputMax; //calculates the max page number
        display(); //displays the list
    }
    @Override
    public void onReactionAdd(ReactionAddEvent event) { //scrolls through the list when the user reacts
        if (event.getUser().isPresent() && event.getUser().get().isBot()) return; //exit if the reaction is from a bot, not a user
        if (event.getEmoji().equalsEmoji(PREV)) page--;
        else if (event.getEmoji().equalsEmoji(NEXT)) page++;
        else if (event.getEmoji().equalsEmoji(PREV2)) page-=10;
        else if (event.getEmoji().equalsEmoji(NEXT2)) page+=10;
        else return;
        if(page<0) page=0;
        if(page>=pageMax) page=pageMax-1;
        display();
    }
}