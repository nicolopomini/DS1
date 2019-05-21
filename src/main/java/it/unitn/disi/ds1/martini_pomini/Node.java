/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unitn.disi.ds1.martini_pomini;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.disi.ds1.martini_pomini.Message.Priviledge;
import it.unitn.disi.ds1.martini_pomini.Message.Request;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pomo
 */
public class Node extends AbstractActor {
    // variables for the protocol
    private ActorRef holder;
    private boolean using, asked;
    private Queue<ActorRef> request_q;
    
    //variables for the structure
    private final int id;
    private final List<ActorRef> neighbours;
    private Random random;

    public Node(int id, ActorRef[] neighbours) {
        this.id = id;
        this.holder = null;
        this.request_q = new LinkedList<>();
        this.neighbours = new ArrayList<>();
        this.neighbours.addAll(Arrays.asList(neighbours));
        this.using = false;
        this.asked = false;
        this.random = new Random();
    }
    
    static public Props props(int id, ActorRef[] neighbours) {
        return Props.create(Node.class, () -> new Node(id, neighbours));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Node))
            return false;
        Node object = (Node) obj;
        return this.id == object.id;
    }
    
    @Override
    public String toString() {
        return "Node " + this.id + " (\n" +
                "\tHolder: " + this.holder + "\n" +
                "\t#request queue: " + this.request_q.size() + "\n" +
                "\tUsing: " + this.using + "\n" +
                "\tAsked: " + this.asked + "\n)";
    }
    
    /*----------------------NODE'S INTERNAL LOGIC-----------------------------*/
    
    private void doCriticalSection() {
        System.out.println("Node " + this.id + " enters the critical section");
        try {
            Thread.sleep(50 + this.random.nextInt(950));
        } catch (InterruptedException ex) {
            System.err.println("Something went wrong with the sleep of node " + this.id);
        }
        System.out.println("Node " + this.id + " exits the critical section");
    }
    
    private void assignPriviledge() {
        /**
         * Method to send a priviledge message to the node in the head of the queue
         * The node must be the holder of the token
         */
        if(this.holder.equals(getSelf()) && !this.using && !this.request_q.isEmpty()) {
            this.holder = this.request_q.poll();
            this.asked = false;
            if (this.holder.equals(getSelf())) {
                this.using = true;
                this.doCriticalSection();
                this.exitCS();
            }
            else {
                // send priviledge to holder
                this.holder.tell(new Priviledge(), getSelf());
            }
        }
    }
    
    private void makeRequest() {
        /**
         * Method to request the token
         */
        if (!this.holder.equals(getSelf()) && !this.request_q.isEmpty() && !this.asked) {
            this.holder.tell(new Request(), getSelf());
            this.asked = true;
        }
    }
    
    /*----------------------NODE'S MESSAGE HANDLERS---------------------------*/
    private void enterCS() {
        /**
         * To enter the CS, the node enqueues itself in its request list
         * Then it tries to enter, in case it is the holder
         * Otherwise, a request is sent
         * Due to the conditions, only one on the two methods will be effectively executed
         */
        this.request_q.add(getSelf());
        this.assignPriviledge();
        this.makeRequest();
    }
    
    private void handleRequestMessage() {
        /**
         * The node receives a Request message
         * the sender is added in the queue
         * Again, due to the conditions, only one on the two methods will be effectively executed
         */ 
        this.request_q.add(getSender());    //non sono sicuro che funzioni
        this.assignPriviledge();
        this.makeRequest();
    }
    
    private void handlePriviledgeMessage() {
        
    }
    
    private void exitCS() {}

    @Override
    public Receive createReceive() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
    
    
}
