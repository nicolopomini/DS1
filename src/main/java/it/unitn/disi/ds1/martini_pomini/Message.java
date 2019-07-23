/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unitn.disi.ds1.martini_pomini;

import akka.actor.ActorRef;
import java.io.Serializable;
import java.util.*;

/**
 *
 * @author pomo
 */
public class Message {
    // Message to send the token to a node
    public static class Privilege implements Serializable {}
    
    // Message to request the token
    public static class Request implements Serializable {}
    
    // Message to request a node to enter the CS
    public static class Enter implements Serializable {}

    // Message to request a node to exit the CS
    public static class ExitCS implements Serializable {}

    // Start message that informs every participant about its neighbours
    public static class Startup implements Serializable {
        public final List<ActorRef> neighbours;   // an array of neighbours members
        public Startup(List<ActorRef> group) {
            this.neighbours = Collections.unmodifiableList(new ArrayList<>(group));
        }
    }
    
    // message to inject the token into a node
    public static class Inject implements Serializable {}
    
    // message to spread the token holder
    public static class Spread implements Serializable {}

    // message to request the status of a node
    public static class Status implements Serializable {}

    // message to make a node fail
    public static class Fail implements Serializable {}

    // start the recovery procedure
    public static class RecoveryWait implements Serializable {}

    //complete recovery procedure
    public static class Recovery implements Serializable {}

    // message to ask for the ok in restarting the node
    public static class Restart implements Serializable {}

    // message to answer ok in restarting the node
    public static class Advise implements Serializable {
        public ActorRef holder, self;
        public boolean asked;
        public LinkedList<ActorRef> request_q;
        public Advise(ActorRef self, ActorRef holder, LinkedList<ActorRef> request_q, boolean asked) {
            this.self = self;
            this.holder = holder;
            this.request_q = (LinkedList<ActorRef>) request_q.clone();
            this.asked = asked;
        }
    }

    //public static class Name implements Serializable {}
}
