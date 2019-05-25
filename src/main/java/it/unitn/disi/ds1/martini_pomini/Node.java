/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unitn.disi.ds1.martini_pomini;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.disi.ds1.martini_pomini.Message.Enter;
import it.unitn.disi.ds1.martini_pomini.Message.Inject;
import it.unitn.disi.ds1.martini_pomini.Message.Priviledge;
import it.unitn.disi.ds1.martini_pomini.Message.Request;
import it.unitn.disi.ds1.martini_pomini.Message.Spread;
import it.unitn.disi.ds1.martini_pomini.Message.Startup;
import it.unitn.disi.ds1.martini_pomini.Message.Status;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 *
 * @author pomo
 */
public class Node extends AbstractActor {
    // variables for the protocol
    private ActorRef holder;
    private boolean using, asked;
    private final Queue<ActorRef> request_q;
    
    //variables for the structure
    private final int id;
    private final List<ActorRef> neighbours;
    private final Random random;

    public Node(int id) {
        this.id = id;
        this.holder = null;
        this.request_q = new LinkedList<>();
        this.neighbours = new ArrayList<>();
        this.using = false;
        this.asked = false;
        this.random = new Random();
        System.out.println("Node " + this.id + " started");
    }
    
    static public Props props(int id) {
        return Props.create(Node.class, () -> new Node(id));
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
        System.out.println("Node " + this.id + ": assignPriviledge");
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
        System.out.println("Node " + this.id + " makeRequest");
        if (!this.holder.equals(getSelf()) && !this.request_q.isEmpty() && !this.asked) {
            this.holder.tell(new Request(), getSelf());
            this.asked = true;
        }
    }
        
    /*----------------------NODE'S MESSAGE HANDLERS---------------------------*/
    
    private void enterCS(Enter msg) {
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
    
    private void handleRequestMessage(Request msg) {
        /**
         * The node receives a Request message
         * the sender is added in the queue
         * Again, due to the conditions, only one on the two methods will be effectively executed
         */ 
        this.request_q.add(getSender());    //non sono sicuro che funzioni
        System.out.println("Node " + this.id + "queue: " + this.request_q.size() + " " + this.request_q);
        this.assignPriviledge();
        this.makeRequest();
    }
    
    private void handlePriviledgeMessage(Priviledge msg) {
        /**
         * The node becomes the holder
         */
        this.holder = getSelf();
        this.assignPriviledge();
        this.makeRequest();
    }
    
    private void exitCS() {
        /**
         * The node exits the CS. The priviledge is passed to another node
         */
        this.using = false;
        this.assignPriviledge();
        this.makeRequest();
    }
    
    private void handleStartup(Startup msg) {
        /**
         * Message received on startup, to get the list of neighbours
         */
        msg.neighbours.forEach((n) -> { 
            this.neighbours.add(n);
        });
        System.out.println("Node " + this.id + ": startup received. #Neighbors: " + this.neighbours.size() + "\n" + this.toString());
    }
    
    private void receiveToken(Inject msg) {
        /**
         * The app manager injects the token in this node
         */
        this.holder = getSelf();
        // inform all the neighbours
        this.neighbours.forEach((n) -> {
            n.tell(new Spread(), getSelf());
        });
        System.out.println("Node " + this.id + ": token received from manager. Node " + this.id + " now holds the token.");
    }
    
    private void getHolderInformation(Spread msg) {
        this.holder = getSender();
        this.neighbours.forEach((n) -> {
            if (!n.equals(this.holder))
                n.tell(new Spread(), getSelf());
        });
        System.out.println("Node " + this.id + ": token info received from another node. The holder for node " + this.id + " is " + this.holder);
    }
    
    private void provideStatus(Status msg) {
        System.out.println(this.toString());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Priviledge.class, this::handlePriviledgeMessage)
                .match(Request.class, this::handleRequestMessage)
                .match(Enter.class, this::enterCS)
                .match(Startup.class, this::handleStartup)
                .match(Inject.class, this::receiveToken)
                .match(Spread.class, this::getHolderInformation)
                .match(Status.class, this::provideStatus)
                .build();
    }
    
    
    
    
    
}
