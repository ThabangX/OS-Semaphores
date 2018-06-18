import java.io.FileNotFoundException;
import java.util.concurrent.Semaphore;
import java.util.Random;
import java.util.*;
import java.io.File;


/*
 * This class serves as a simulator for the Taxi moving about in branches colecting people
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author USER
 */
public final class Simulator extends Thread {
    
    //variable declartions
    protected static Semaphore instance   = new Semaphore(1);
    protected static Semaphore taxiQueue= new Semaphore(1);
    private static Person[] persons;
    //Taxi
    private static Taxi taxi;
    private final String file;

    //contructor
    public Simulator(String file) {
        this.file = file;
        readData() ;
    }

//read data from file
    public void readData() {
        int numberOFbranches ; 
        int numberOFpeople; 
      

        try
        {
            
            File infile;
            infile = new File(file);
            Scanner scanner;
            scanner = new Scanner(infile);

            numberOFpeople = scanner.nextInt();
            numberOFbranches = scanner.nextInt();

            taxi = new Taxi(numberOFbranches); 
            persons = new Person[numberOFpeople];

            int counter;
            counter = 0;

            while (counter < numberOFpeople && scanner.hasNextInt())
            {
                int k;
                k = scanner.nextInt();
                ArrayList<Long>   times;
                times = new ArrayList<>();
                ArrayList<Integer> locations = new ArrayList<>();
                

                String[] data = scanner.nextLine().split(",");
                int endpoints = data.length;

                for (int i = 0; i<endpoints/2; i++)
                {
                    
                    int b;
                    b = new Scanner(data[i * 2]).useDelimiter("\\D+").nextInt();
                    
                    long d;
                    d = new Scanner(data[i * 2 + 1]).useDelimiter("\\D+").nextInt();

                    locations.add(b);
                    times.add(d);
                }
                Person newPerson = new Person(k, taxi, locations,times);
                persons[counter] = newPerson;
                counter++;
                System.out.println("p: ( " + persons[k].getID() + ", b: " + locations + ", d: " + times + " )");
            }
        }
        catch (FileNotFoundException hc) {
            System.out.println("file '"+file+"' not found!");
        }
        catch (Exception g) {
            String toString = g.toString();
        }
    }


    @Override
    public void run()
    {
       
        taxi.start();
        
        for (Person person : persons) {
            try {
                person.start();
                try { sleep( new Random().nextInt((3300/100)) );  } catch(InterruptedException ex) {}
            }catch (java.lang.NullPointerException ec) { }
        }
    }
//run the simulator
    
    public static void main (String [] args)
    {
        if (args.length <= 0) {
            System.out.println("Missing argument, to run simulator use: 'java Simulator filename'");
        }
        else {
            Simulator simulator = new Simulator(args[0]);
            simulator.start();
        }
    }    
}
