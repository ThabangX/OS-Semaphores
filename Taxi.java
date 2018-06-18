import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;
import java.util.concurrent.Semaphore;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author USER
 */
public class Taxi extends Thread{
    
    //variables for the Taxi
    private final Semaphore instance   = Simulator.instance;
    private final Semaphore taxiQueue = Simulator.taxiQueue;
    //the state of the taxi
    private enum stateTaxi { waiting, ready, running }
    //moving about, back or fore.
    private enum Route { in, out } 
    private stateTaxi state;
    private Route route;

//mutext lock
    private Lock lock ;

    private final int nBranches;
    private int thisBranch;
    private int prev_Branch;
    private int nthBranch;

//give number associates
    private final int hail=1;
    //second call
    private final int request=2;
    //third call
    private final int depart=3;
    //fourth call
    private final int arrive=4;
    //fifth call
    private final int pickup=5;
    //last call
    private final int dropped=6;

    
    private final long st = 9;
    private long cur_Time = st;
    private long hrs = st;
    private long mins = 0;
    private long pdt = 33 ;
    
    private int thisBranchrequests = 0;
    private int thisBranchPicked = 0;

    private final Deque<Person> queueH; 
    private final Deque<Person> pickQ; 
    private final HashMap<Integer, Person> reqQ; 



//constructor for the taxi
    public Taxi (int nBranches) {

        this.queueH = new ArrayDeque<>();
        this.pickQ = new ArrayDeque<>();
        this.reqQ = new HashMap<>();
        this.nBranches = nBranches;
        this.thisBranch =0;
        this.prev_Branch=0;
        this.lock = new ReentrantLock();
    }




    public synchronized void requestBranch(Person person) throws InterruptedException
    {
        lock.lock();
        try {
            synchronized(reqQ)
            {
                if (!pickQ.contains(person))
                {
                } else {
                    taxiQueue.acquire();
                    trace(request, person);
                    reqQ.put(person.getID(), person);
                    person.setState(Person.pState.request);
                    thisBranchrequests++;
                    taxiQueue.release();
                    instance.release();
                }
            }
        } finally {
            lock.unlock();
        }
    }


    //get the taxi to move


    @Override
    public String toString() {
        return "Taxi 1" + "( Branch: "+ thisBranch + ",  State: " + state + ", Direction: "+route+" )";
    }
    
     public void move() throws InterruptedException
    {
        int hq = 0;
        long tTime;
        tTime = 33*2;
        this.nthBranch = nBranches-1;

        lock.lock();
        try {
            if ( !queueH.isEmpty() || (!reqQ.isEmpty() && state == stateTaxi.ready) &&
                    thisBranchPicked == thisBranchrequests )
            {
                this.timeWait(tTime);
                trace(depart);
                state = stateTaxi.running;
                
                prev_Branch = thisBranch;
                
                if (this.route == Route.out)
                {
                    if (thisBranch < nthBranch)
                        thisBranch++;
                    else if (thisBranch == nthBranch) {
                        thisBranch--;
                        this.route = Route.in;
                    }
                }
                else {
                    if (thisBranch <= hq) {
                        if (thisBranch != hq) {
                        } else {
                            thisBranch++;
                            this.route = Route.out;
                        }
                    } else thisBranch--;
                }
                Iterator<Person> itr = reqQ.values().iterator();
                
                while ( itr.hasNext() )
                {
                    
                    Person pPerson = reqQ.get(itr.next().getID());
                    
                    if ( pPerson.getStatePerson()!= Person.pState.request) {
                    } else {
                        pPerson.taxiMoving(thisBranch);
                    }
                }
                trace(arrive);
                thisBranchrequests=0;
                thisBranchPicked=0;
                state = stateTaxi.waiting;
            }
            else {
                state = stateTaxi.waiting;
            }
        } finally {
            lock.unlock();
        }
    }

//run method for Thread, call start to actually run it
    @Override
    public void run ()
    {
        route = Route.out;
        state = stateTaxi.waiting;

        while(true)
        {
            try {
               
                while ( !pickQ.isEmpty() || !queueH.isEmpty() || !reqQ.isEmpty() ) {
                    liftOn();
                    liftoff();
                    move();

                    sleep(33); 
                }
            }
            catch (InterruptedException e) {
                System.out.println(e);
            }
        }
    }

   
    public void liftoff () throws InterruptedException
    {
        lock.lock();
        try {
            if ((state != stateTaxi.waiting || prev_Branch == thisBranch) || reqQ.size()<=0)
            {
            } else {
                synchronized (pickQ)
                {
                    
                    taxiQueue.acquire();
                    for (Person p: pickQ)
                    {
                        assert (p!= null);
                        if (reqQ.containsValue(p))
                        {
                            Person otherp = reqQ.get(p.getID());
                            int destineBranch = otherp.getDestinationBranch();
                            long waitDuration = otherp.getDestinationTime();
                            if ( otherp.getStatePerson() != Person.pState.travel || destineBranch != thisBranch)
                            {
                            } else {
                                otherp.dropPerson(waitDuration/33);
                                pickQ.remove(p);
                                reqQ.remove(otherp.getID());
                                trace(dropped,otherp);
                            }
                        }
                    }
                    taxiQueue.release();
                }
            }
            state = stateTaxi.ready;           
        } finally {
            lock.unlock();
        }
    }

    public String clock(long time)
    {
        mins += time/33 ;
        if (mins<60) {
        } else {
            hrs += 1;
            if (hrs<24) {
            } else {
                hrs=0;
            }
            mins = 0;
        }
        cur_Time +=33;
        return hrs +":"+ mins;
    }

    //get on the taxi
    public void liftOn () throws InterruptedException
    {
        lock.lock();
        try {
            this.timeWait(pdt);
            
            if ((state == stateTaxi.waiting) && !queueH.isEmpty())
            {
                synchronized (queueH)
                {

                    taxiQueue.acquire();
                    for (Person person: queueH)
                    {
                        if ((person.getStatePerson() == Person.pState.hail) && (person.getThisBranch() == thisBranch))
                        {
                            person.setState(Person.pState.pickup);
                            pickQ.add(person);
                            queueH.remove(person);
                            trace(pickup,person);
                            thisBranchPicked++;
                        }
                    }
                    taxiQueue.release();
                }
            }
        } finally {
            lock.unlock();
        }
    }
    public void timeWait(long time)
    {
        try { sleep (time ); }
        catch (InterruptedException e) {}
    }
   

    public synchronized void hailingTaxi(Person p) throws InterruptedException
    {
        lock.lock();
        try {
            synchronized (queueH) {
                taxiQueue.acquire();
                trace(hail, p);
                queueH.add(p);
                p.setState(Person.pState.hail);
                taxiQueue.release();
                instance.release();
            }
        } finally {
            lock.unlock();
        }
    }



    
    



    public void trace (int option, Object... varargs)
    {
        String trigger;
        switch (option) {
            case hail:
                int identity;
        identity = ((Person)varargs[0]).getID();
                int cbranch;
        cbranch = ((Person)varargs[0]).getThisBranch();
                trigger = String.format("%s branch %d : person %d hail",clock(cur_Time), cbranch,identity);
                break;
            case request:
                int identityTwo;
        identityTwo = ((Person)varargs[0]).getID();
                int c2branch;
        c2branch = ((Person)varargs[0]).getThisBranch();
                int d2branch;
        d2branch = ((Person)varargs[0]).getDestinationBranch();
                trigger = String.format("%s branch %d : person %d request %d", clock(cur_Time), c2branch, identityTwo, d2branch);
                break;
            case depart:
                trigger = String.format("%s branch %d : taxi depart", clock(cur_Time),thisBranch );
                break;
            case arrive:
                trigger = String.format("%s branch %d : taxi arrive ", clock(cur_Time), thisBranch );
                break;
            case pickup:
                trigger = String.format("%s branch %d : PICKEDUP person %d", clock(cur_Time), thisBranch, ((Person)varargs[0]).getID() );
                break;
            case dropped:
                trigger = String.format("%s branch %d : DISEMBARK person %d", clock(cur_Time), thisBranch, ((Person)varargs[0]).getID() );
                break;

            default:
                trigger = "ERROR UNKNOWN EVENT!";
        }
        System.out.printf(trigger+"\n");
    }
}
