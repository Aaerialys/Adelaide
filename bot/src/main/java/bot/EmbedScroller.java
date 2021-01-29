package bot;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;

public class EmbedScroller implements ReactionAddListener { //a message embedded with a list of many items that the user can scroll through
    private static final String PREV = "\u2b05", NEXT = "\u27A1"; //unicode values for the left and right arrow emoji
    private EmbedBuilder embed; //the embed to display
    private String listTitle; //the title of the list
    private String[] scrollList; //the contents of the list
    private Message message; //the message to put the embed in
    private int page, outputMax, pageMax; //page: current page number, outputMax: max list items per page, pageMax: total page number of the list
    
    private void display() { //displays a page of the list
        String output = new String();
        for (int i = page * outputMax; i < Math.min((page + 1) * outputMax, scrollList.length); i++) output += scrollList[i]; //adds all the list items on the current page to the output
        final String output2 = output;
        message.edit(embed.setFooter("Page " + (page + 1) + "/" + pageMax) //edits the message to contain the output
            .updateFields(field -> {
                return field.getName().equals(listTitle);
            }, field -> {
                field.setValue(output2);
            }));
    }
    
    public EmbedScroller(Message message, EmbedBuilder embed, String listTitle, String[] scrollList, int outputMax) { //constructor
        message.addReaction("\u2b05"); //adds the left arrow emoji to the message for scrolling
        message.addReaction("\u27a1"); //adds the right arrow emoji to the message for scrolling
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
    @Override
    public void onReactionAdd(ReactionAddEvent event) { //scrolls through the list when the user reacts
        if (event.getUser().isPresent() && event.getUser().get().isBot()) return; //exit if the reaction is from a bot, not a user
        if (event.getEmoji().equalsEmoji(PREV)) { //if the user reacted with a left arrow, decrease the page number unless it is already at 0
            if (page > 0) page--;
            display(); //display the new list page
        } else if (event.getEmoji().equalsEmoji(NEXT)) { //if the user reacted with a right arrow, increase the page number unless it is already at the max page
            if (page < pageMax - 1) page++;
            display(); //display the new page
        }
    }
}