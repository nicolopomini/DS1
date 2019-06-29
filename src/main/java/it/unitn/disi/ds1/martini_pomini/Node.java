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
import it.unitn.disi.ds1.martini_pomini.Message.Fail;
import it.unitn.disi.ds1.martini_pomini.Message.Restart;
import it.unitn.disi.ds1.martini_pomini.Message.Name;
import it.unitn.disi.ds1.martini_pomini.Message.Advise;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 *
 * @author pomo
 */


public class Node extends AbstractActor {
    // variables for the protocol
    private ActorRef holder;
    private boolean using, asked, recovery;
    private final Queue<ActorRef> request_q;
    
    //variables for the structure
    private final int id, minCSTime, maxCSTime, downTime, startRecoveryTime;
    private final List<ActorRef> neighbours;
    private final Random random;

    public Node(int id, int minCSTime, int maxCSTime, int downTime, int startRecoveryTime) {
        this.id = id;
        this.holder = null;
        this.request_q = new LinkedList<>();
        this.neighbours = new ArrayList<>();
        this.using = false;
        this.asked = false;
        this.recovery = false;
        this.random = new Random();
        this.minCSTime = minCSTime;
        this.maxCSTime = maxCSTime;
        this.downTime = downTime;
        this.startRecoveryTime = startRecoveryTime;
        System.out.println("Node " + this.id + " started");
    }

    static public Props props(int id, int minCSTime, int maxCSTime, int downTime, int startRecoveryTime) {
        return Props.create(Node.class, () -> new Node(id, minCSTime, maxCSTime, downTime, startRecoveryTime));
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
        int hold = -1;
        try {
            hold = (int) Await.result(ask(this.holder,new Name(),1000),Duration.create(1000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            //e.printStackTrace;
        }

        return "Node " + this.id + " (\n" +
                "\tHolder: " + ((hold == -1) ? this.holder : hold) + "\n" +
                "\t#request queue: " + this.request_q.size() + "\n" +
                "\tUsing: " + this.using + "\n" +
                "\tAsked: " + this.asked + "\n)";

    }
    
    /*----------------------NODE'S INTERNAL LOGIC-----------------------------*/
    
    private void doCriticalSection() {
        System.out.println("Node " + this.id + " enters the critical section");
        try {
            Thread.sleep(this.minCSTime + this.random.nextInt(this.maxCSTime - this.minCSTime));
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
        if (!this.recovery) {
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
                    try {
                        System.out.println("Node " + this.id + " is assigning privilege to node " + Await.result(ask(this.holder,new Name(),1000),Duration.create(1000, TimeUnit.MILLISECONDS)));
                        //System.out.println("Node " + this.id + " is assigning privilege to node " + this.holder);
                    } catch (Exception e) {
                        //e.printStackTrace();
                        System.out.println("Node " + this.id + " is assigning privilege to node " + this.holder);
                    }
                    this.holder.tell(new Priviledge(), getSelf());
                }
            }
        }
    }

    private void makeRequest() {
        /**
         * Method to request the token
         */
        if (!this.recovery) {
            if (!this.holder.equals(getSelf()) && !this.request_q.isEmpty() && !this.asked) {
                try {
                    System.out.println("Node " + this.id + " is making a request to node " + Await.result(ask(this.holder,new Name(),1000),Duration.create(1000, TimeUnit.MILLISECONDS)));
                    //System.out.println("Node " + this.id + " is making a request to node " + this.holder);
                } catch (Exception e) {
                    System.out.println("Node " + this.id + " is making a request to node " + this.holder);
                    //e.printStackTrace();
                }
                this.holder.tell(new Request(), getSelf());
                this.asked = true;
            }
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
        System.out.println("Node " + this.id + " queue contains #" + this.request_q.size());

        if (this.request_q.size() > 0) {
            System.out.println("\telements: ");
            for (ActorRef element : this.request_q) {
                try {
                    System.out.println("\t\tNode " + Await.result(ask(element,new Name(),1000),Duration.create(1000, TimeUnit.MILLISECONDS)));
                    //System.out.println("\t\t" + this.holder);
                } catch (Exception e) {
                    //e.printStackTrace();
                    System.out.println("\t\t" + this.holder);
                }

            }
        }

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

    /*----------------------NODE'S INTERACTION LOGIC-----------------------------*/

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
        try {
            System.out.println("Node " + this.id + ": token info received from another node. The holder for node " + this.id + " is " + Await.result(ask(this.holder,new Name(),1000),Duration.create(1000, TimeUnit.MILLISECONDS)));
            //System.out.println("Node " + this.id + ": token info received from another node. The holder for node " + this.id + " is " + this.holder);
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("Node " + this.id + ": token info received from another node. The holder for node " + this.id + " is " + this.holder);

        }
    }

    private void simulateFailure(Fail msg) {
        /**
         * It simulates the failure of the node. In the end it start the recovery procedure.
         */
        if (!this.using && !this.recovery) {
            System.out.println("Node " + this.id + " has failed and now is down.");

            //simulating the loss of data.
            this.holder = null;
            this.request_q.clear();
            this.asked = false;

            try {
                Thread.sleep(this.downTime);
            } catch (InterruptedException ex) {
                System.err.println("Something went wrong with the sleep of node " + this.id);
            }
            System.out.println("Node " + this.id + " has restarted.");
            this.recoveryProcedure();
            this.assignPriviledge();
            this.makeRequest();
        }
    }

    private void recoveryProcedure() {
        /**
         * The recovery procedure for a failed node. In the end, the node is fully restored
         */
        this.recovery = true;
        System.out.println("Node " + this.id + " has started the recovery procedure");
        //delay, to ensure that all the message sent by this node has been received by the other nodes.
        try {
            Thread.sleep(this.startRecoveryTime);
        } catch (InterruptedException ex) {
            System.err.println("Something went wrong with the sleep of node " + this.id);
        }

        int count = 1;
        LinkedList<Advise> responses = new LinkedList<>();

        while ((responses.size() != this.neighbours.size()) && count < 6) {
            responses.clear();
            for (ActorRef n : this.neighbours) {
                try {
                    int time = 1000 * count;
                    //System.out.println("Asking " + n + " to acknoledge the restart operation");
                    responses.add((Advise) Await.result(ask(n, new Restart(), time), Duration.create(time, TimeUnit.MILLISECONDS)));
                    //System.out.println("Node " + n + " acknoledged the restart operation");
                } catch (Exception e) {
                    System.out.println("One or more nodes has not answered yet. Retrying for the #" + count + " time");
                    count++;
                    e.printStackTrace();
                }
            }
        }

        if (responses.size() != this.neighbours.size()) {
            System.out.println("After "+count+" tentatives one or more node did not acknoledged the restart operation of the node " + this.id + ".\nThis is an unexpected situation, the program will now terminate.");
            System.exit(1);
        }
        System.out.println("Node " + this.id + " has informed all the neighbours that the recovery procedure has started.");

        //Determining the holder and asked and rebuilding the request_q.
        boolean allEquals = true;
        ActorRef supposedHolder = null;
        LinkedList<ActorRef> privilegedNodeQueue = new LinkedList<>();
        for (Advise resp : responses) {
            if (!resp.holder.equals(getSelf())) {
                //System.out.println("Resp holder " + resp.holder);
                allEquals = false;
                supposedHolder = resp.self;
                privilegedNodeQueue = (LinkedList<ActorRef>) resp.request_q.clone();
            } else {
                if (resp.asked) {
                    this.request_q.add(resp.self);
                }
            }
        }

        if (allEquals) {
            this.holder = getSelf();
            this.asked = false;
        } else {
            this.holder = supposedHolder;
            this.asked = privilegedNodeQueue.contains(getSelf());
        }

        System.out.println("Node " + this.id + " has terminated with success the recovery procedure. The values of holder, asked and request_q has been correctly inferred.");
        this.recovery = false;
    }

    private void adviseMessage(Restart msg) {
        getSender().tell(new Advise(getSelf(),this.holder, (LinkedList<ActorRef>) this.request_q, this.asked), getSelf());
    }

    private void answerName(Name msg) {
        getSender().tell(this.id,getSelf());
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
                .match(Fail.class, this::simulateFailure)
                .match(Name.class, this::answerName)
                .match(Restart.class, this::adviseMessage)
                .build();
    }
}
