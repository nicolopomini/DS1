/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unitn.disi.ds1.martini_pomini;
import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import it.unitn.disi.ds1.martini_pomini.Message.Enter;
import it.unitn.disi.ds1.martini_pomini.Message.ExitCS;
import it.unitn.disi.ds1.martini_pomini.Message.Inject;
import it.unitn.disi.ds1.martini_pomini.Message.Privilege;
import it.unitn.disi.ds1.martini_pomini.Message.Request;
import it.unitn.disi.ds1.martini_pomini.Message.Spread;
import it.unitn.disi.ds1.martini_pomini.Message.Startup;
import it.unitn.disi.ds1.martini_pomini.Message.Status;
import it.unitn.disi.ds1.martini_pomini.Message.Fail;
import it.unitn.disi.ds1.martini_pomini.Message.Restart;
import it.unitn.disi.ds1.martini_pomini.Message.Recovery;
import it.unitn.disi.ds1.martini_pomini.Message.RecoveryWait;
import it.unitn.disi.ds1.martini_pomini.Message.Advise;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import akka.actor.Props;
import scala.concurrent.duration.Duration;
import scala.concurrent.Await;

/**
 *
 * @author pomo
 */


public class Node extends AbstractActor {
    private ActorSystem system;

    // variables for the protocol
    private ActorRef holder;
    private boolean using, asked, recovery, down;
    private final Queue<ActorRef> request_q;
    
    //variables for the structure
    private final int id, minCSTime, maxCSTime, downTime, startRecoveryTime;
    private final List<ActorRef> neighbours;
    private final Random random;

    public Node(ActorSystem system, int id, int minCSTime, int maxCSTime, int downTime, int startRecoveryTime) {
        this.system = system;
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

    static public Props props(ActorSystem system, int id, int minCSTime, int maxCSTime, int downTime, int startRecoveryTime) {
        return Props.create(Node.class, () -> new Node(system, id, minCSTime, maxCSTime, downTime, startRecoveryTime));
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
        return "Node " + this.id + "(Reference:" + getSelf() + ") {\n" +
                "\tHolder: " + ((hold == -1) ? this.holder : hold) + "\n" +
                "\t#request queue: " + this.request_q.size() + "\n" +
                "\tUsing: " + this.using + "\n" +
                "\tAsked: " + this.asked + "\n}";

    }
    
    /*----------------------NODE'S INTERNAL LOGIC-----------------------------*/
    
    private void doCriticalSection() {
        assert (!this.down && !this.recovery);
        System.out.println("Node " + this.id + " enters the critical section");
        this.system.scheduler().scheduleOnce(Duration.create(this.minCSTime + this.random.nextInt(this.maxCSTime - this.minCSTime), TimeUnit.MILLISECONDS), new Runnable() {
            @Override
            public void run() {
                getSelf().tell(new ExitCS(), ActorRef.noSender());
            }
        }, system.dispatcher());
    }
    
    private void assignPrivilege() {
        /**
         * Method to send a privilege message to the node in the head of the queue
         * The node must be the holder of the token
         */
        if (!this.recovery && !this.down) {
            if(this.holder.equals(getSelf()) && !this.using && !this.request_q.isEmpty()) {
                this.holder = this.request_q.poll();
                this.asked = false;
                if (this.holder.equals(getSelf())) {
                    this.using = true;
                    this.doCriticalSection();
                }
                else {
                    // send priviledge to holder
                    try {
                        //System.out.println("Node " + this.id + " is assigning privilege to node " + Await.result(ask(this.holder,new Name(),1000),Duration.create(1000, TimeUnit.MILLISECONDS)));
                        System.out.println("Node " + this.id + " is assigning privilege to node " + this.holder);
                    } catch (Exception e) {
                        //e.printStackTrace();
                        System.out.println("Node " + this.id + " is assigning privilege to node " + this.holder);
                    }
                    this.holder.tell(new Privilege(), getSelf());
                }
            }
        }
    }

    private void makeRequest() {
        /**
         * Method to request the token
         */
        if (!this.recovery && !this.down) {
            if (!this.holder.equals(getSelf()) && !this.request_q.isEmpty() && !this.asked) {
                try {
                    //System.out.println("Node " + this.id + " is making a request to node " + Await.result(ask(this.holder,new Name(),1000),Duration.create(1000, TimeUnit.MILLISECONDS)));
                    System.out.println("Node " + this.id + " is making a request to node " + this.holder);
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
        assert (!this.down && !this.recovery);
        this.request_q.add(getSelf());
        this.assignPrivilege();
        this.makeRequest();
    }
    
    private void handleRequestMessage(Request msg) {
        /**
         * The node receives a Request message
         * the sender is added in the queue
         * Again, due to the conditions, only one on the two methods will be effectively executed
         */
        if (!this.down) {
            this.request_q.add(getSender());    //non sono sicuro che funzioni
            String print = "Node " + this.id + " queue contains #" + this.request_q.size() + "\n";

            if (this.request_q.size() > 0) {
                print += "\telements: \n";
                for (ActorRef element : this.request_q) {
                    print += "\t\t" + element + "\n";
                }
            }
            System.out.print(print);
            this.assignPrivilege();
            this.makeRequest();
        }
    }
    
    private void handlePriviledgeMessage(Privilege msg) {
        /**
         * The node becomes the holder
         */
        if (!this.down) {
            this.holder = getSelf();
            this.assignPrivilege();
            this.makeRequest();
        }
    }
    
    private void exitCS(ExitCS msg) {
        /**
         * The node exits the CS. The priviledge is passed to another node
         */
        assert (this.using && !this.down && !this.recovery);
        System.out.println("Node " + this.id + " exits the critical section");
        this.using = false;
        this.assignPrivilege();
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
        System.out.println("Node " + this.id + ": token info received from another node. The holder for node " + this.id + " is " + this.holder);
    }

    private void simulateFailure(Fail msg) {
        /**
         * It simulates the failure of the node. In the end it start the recovery procedure.
         */
        if (!this.using && !this.recovery) {
            System.out.println("Node " + this.id + " has failed and now is down.");
            this.down = true;
            //simulating the loss of data.
            this.holder = null;
            this.request_q.clear();
            this.asked = false;
            this.system.scheduler().scheduleOnce(Duration.create(this.downTime, TimeUnit.MILLISECONDS), new Runnable() {
                @Override
                public void run() {
                    getSelf().tell(new RecoveryWait(), ActorRef.noSender());
                }
            }, system.dispatcher());
        }
    }

    private void recoveryStart(RecoveryWait msg) {
        this.down = false;
        this.recovery = true;
        System.out.println("Node " + this.id + " has restarted.");
        System.out.println("Node " + this.id + " has started the recovery procedure");

        this.system.scheduler().scheduleOnce(Duration.create(this.startRecoveryTime, TimeUnit.MILLISECONDS), new Runnable() {
            @Override
            public void run() {
                getSelf().tell(new Recovery(), ActorRef.noSender());
            }
        }, system.dispatcher());
    }

    private void recoveryProcedure(Recovery msg) {
        /**
         * The recovery procedure for a failed node. In the end, the node is fully restored
         */
        int count = 1;
        LinkedList<Advise> responses = new LinkedList<>();

        while ((responses.size() != this.neighbours.size()) && count < 6) {
            responses.clear();
            for (ActorRef n : this.neighbours) {
                try {
                    int time = 1000 * count;
                    responses.add((Advise) Await.result(ask(n, new Restart(), time), Duration.create(time, TimeUnit.MILLISECONDS)));
                } catch (Exception e) {
                    System.out.println("One or more nodes has not answered yet. Retrying for the #" + count + " time");
                    count++;
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
        this.assignPrivilege();
        this.makeRequest();
    }

    private void adviseMessage(Restart msg) {
        getSender().tell(new Advise(getSelf(),this.holder, (LinkedList<ActorRef>) this.request_q, this.asked), getSelf());
    }

    private void provideStatus(Status msg) {
        System.out.println(this.toString());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Privilege.class, this::handlePriviledgeMessage)
                .match(Request.class, this::handleRequestMessage)
                .match(Enter.class, this::enterCS)
                .match(Startup.class, this::handleStartup)
                .match(Inject.class, this::receiveToken)
                .match(Spread.class, this::getHolderInformation)
                .match(Status.class, this::provideStatus)
                .match(Fail.class, this::simulateFailure)
                .match(Recovery.class, this::recoveryProcedure)
                .match(RecoveryWait.class, this::recoveryStart)
                .match(Restart.class, this::adviseMessage)
                .match(ExitCS.class, this::exitCS)
                .build();
    }
}
