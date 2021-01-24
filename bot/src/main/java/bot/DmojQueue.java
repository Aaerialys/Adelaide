package bot;

public class DmojQueue { //a queue that stores dmoj api call times
    private final int SIZE = 100; //the maximum queue size, will never be exceeded due to dmoj rate limit of 90/minute
    private int start, end; //the front and back positions of the queue
    private long[] vals = new long[SIZE]; //the values stored in the queue
    
    public DmojQueue() {
        start = 0;
        end = -1; //initializes the start and end values
    }
    
    public void push(long newVal) { //insert a new value to the end of the queue
        end++;
        if (end == SIZE) end = 0; //increments the end positions, wrapping back around to 0 if it exceeds the max size
        vals[end] = newVal; //set the new value in the end position
    }
    
    public long pop() { //removes and returns the queue's front value
        long val = vals[start]; //stores the current front value to be removed
        start++; //increments the start position, wrapping around to 0 if it exceeds the max size
        if (start == SIZE) start = 0;
        return val; //returns the removed value
    }
    
    public long front() { //returns the queue's front value
        return vals[start];
    }
    
    public int size() { //returns the number of elements in the queue
        return (end - start + 1 + SIZE) % SIZE;
    }
}