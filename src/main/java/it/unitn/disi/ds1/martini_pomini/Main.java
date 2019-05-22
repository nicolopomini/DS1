/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unitn.disi.ds1.martini_pomini;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.ArrayList;
import java.util.List;
import it.unitn.disi.ds1.martini_pomini.Message.Startup;

/**
 *
 * @author pomo
 */
public class Main {
    public static void main(String[] a) {
        /*
        final ActorSystem system = ActorSystem.create("ds1project");
        List<ActorRef> group = new ArrayList<>();
        group.add(system.actorOf(Node.props(0)));
        group.add(system.actorOf(Node.props(1)));
        
        List<ActorRef> l0 = new ArrayList<>();
        l0.add(group.get(1));
        Startup start0 = new Startup(l0);
        group.get(0).tell(start0, null);
        
        List<ActorRef> l1 = new ArrayList<>();
        l1.add(group.get(0));
        Startup start1 = new Startup(l1);
        group.get(1).tell(start1, null);
        
        group.get(0).tell(new Message.Inject(), null);
        */
        Manager manager = new Manager();
        
    }
}
