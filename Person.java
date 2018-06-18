import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import org.omg.Messaging.SyncScopeHelper;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author USER
 */
public class Person extends Thread {
    
   //set a person state
    private pState state;
    //declare an enum to switch between states
    protected enum pState { wait, hail, pickup, request, travel }
    private static Taxi taxi;

    //id 
    private final int id;
    //current branch
    private int thisBranch;
    //all the durations
    private ArrayList<Long>   times = new ArrayList<>();
   //all the destinations
    private ArrayList<Integer> places = new ArrayList<>();


    private final Semaphore instance = Simulator.instance;
    //mutex lock
    private final Lock lock ;
    
    //constructor

    Person (int id, Taxi taxi, ArrayList<Integer> places, ArrayList<Long> times)
    {
        this.id = id ;
        this.taxi = taxi ;
        this.places = places ;
        this.times = times ;
        lock = new ReentrantLock();
        this.thisBranch = 0 ;
        this.state = pState.wait;
    }



//time of arrival
    public long getDestinationTime() { 
        assert (!times.isEmpty());
        return times.get(0);
    }
    //current branch
    @Override
    public String toString() {
        return "Person "+ getID()+ " ( branches: "+ places+",  destinations: "+times+" )";
    }
    public int  getThisBranch() { 
        return thisBranch;
    }
    //set current branch
    public void setThisBranch(int branch ){
        thisBranch = branch; 
    } 

//run method for thread,call start to run it.
    @Override
    public void run()
    {
        try {
           
            while(!places.isEmpty())
            {
               
                if (state!=pState.wait) {
                    if (state==pState.pickup)
                        this.callBranch();
                } else this.taxiHail();

                int time = (3300/100); 
                sleep( new Random().nextInt(time));  
            }

        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    

    //next Branch
    public void nextB(){
        assert (!places.isEmpty());
        places.remove(0); 
    }
    //next destination
    public void nextD(){
        assert (!times.isEmpty());
        times.remove(0);
    }

    public int  getID(){
        return id; }
    //discharge a person
        public synchronized void dropPerson (long durationInBranch) throws InterruptedException {
        lock.lock();
        try {
            state = pState.wait;
            nextB();
            nextD();
            synchronized(this) {
                this.timeWait(durationInBranch);
            }
        } finally {
            lock.unlock();
        }
    }
    
    public int  getDestinationBranch(){
        assert (!places.isEmpty());
        return places.get(0); 
    }

    //waiting time
        public void timeWait(long time) throws InterruptedException {
        try { sleep (time ); }
        catch (InterruptedException e) {}
    }
//set state
    public synchronized void setState(pState state) throws InterruptedException
    {
        this.state = state;
    }
//get the state of person whether waiting etc
    public pState getStatePerson() { return state;}
//hail taxi by person
    public void taxiHail() throws Exception {
        lock.lock();
        try {
            instance.acquire();
            taxi.hailingTaxi(this);
        } finally {
            lock.unlock();
        }
    }
//request Branch
    public void callBranch() throws Exception {
        lock.lock();
        try {
            instance.acquire();
            taxi.requestBranch(this);
        } finally {
            lock.unlock();
        }
    }
//move with the taxi as it move from branch to branch
    public synchronized void taxiMoving (int branch) throws InterruptedException {
        lock.lock();
        try {
            state = pState.travel;
            setThisBranch( branch );
        } finally {
            lock.unlock();
        }
    }
    





    
}
