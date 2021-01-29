package bot;

public class UserProblem extends Problem { //stores problems within a user's to do list
    private static final long serialVersionUID = 7545324917718774402L; //autogenerated for storage as a file
    
    private String status; //the status of the problem in a user's to do list, eg. (thinking, implementing, debugging)
    
    public UserProblem(String link, String status) { //constructs the problem with its link and status
        super(link);
        this.status = status;
    }
    
    void setStatus(String status) { //modifies the status to a new value
        this.status = status;
    }
    
    public String getStatus() { //gets the status
        return status;
    }
    @Override
    public String toStringEmbed() { //adds an additional status tag to the original toStringEmbed() output
        return super.toStringEmbed() + " [" + status + "]";
    }
}