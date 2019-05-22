/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unitn.disi.ds1.martini_pomini;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 *
 * @author pomo
 */
public class Manager {
    public static final String DEFAULT_FILE = "src/main/resources/topology.txt";
    
    private Hashtable<Integer, ArrayList<ActorRef>> edges;
    private ArrayList<ActorRef> nodes;
    private final ActorSystem system;
    
    public Manager(String filepath) {
        this.edges = new Hashtable<>();
        this.nodes = new ArrayList<>();
        this.system = ActorSystem.create("ds1project");
        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(filepath));
            int links = Integer.parseInt(inputFile.readLine());
            for (int i = 0; i < links; i++) {
                String[] link = inputFile.readLine().split(" ");
                int node1id = Integer.parseInt(link[0]);
                int node2id = Integer.parseInt(link[1]);
                // add the nodes if is the first time they are met
                if (!this.edges.containsKey(node1id)) {
                    this.nodes.add(this.system.actorOf(Node.props(node1id)));
                    this.edges.put(node1id, new ArrayList<>());
                }
                if (!this.edges.containsKey(node2id)) {
                    this.nodes.add(this.system.actorOf(Node.props(node2id)));
                    this.edges.put(node2id, new ArrayList<>());
                }
                // adding the connections
                this.edges.get(node1id).add(this.nodes.get(node2id));
                this.edges.get(node2id).add(this.nodes.get(node1id));
            }
        } catch (FileNotFoundException ex) {
            System.err.println("Error in opening the topology file. " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("Error in reading the topology file. " + ex.getMessage());
        }
        
    }
    
    public Manager() {
        this(Manager.DEFAULT_FILE);
    }
}
