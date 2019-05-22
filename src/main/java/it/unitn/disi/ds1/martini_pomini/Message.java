/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unitn.disi.ds1.martini_pomini;

import akka.actor.ActorRef;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author pomo
 */
public class Message {
    // Message to send the token to a node
    public static class Priviledge implements Serializable {}
    
    // Message to request the token
    public static class Request implements Serializable {}
    
    // Message to request a node to enter the CS
    public static class Enter implements Serializable {}
    
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
}
