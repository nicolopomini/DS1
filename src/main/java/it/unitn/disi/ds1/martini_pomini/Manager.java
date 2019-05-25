/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unitn.disi.ds1.martini_pomini;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import it.unitn.disi.ds1.martini_pomini.Message.Enter;
import it.unitn.disi.ds1.martini_pomini.Message.Inject;
import it.unitn.disi.ds1.martini_pomini.Message.Startup;
import it.unitn.disi.ds1.martini_pomini.Message.Status;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

/**
 *
 * @author pomo
 */
public class Manager {
    public static final String DEFAULT_FILE = "src/main/resources/topology.txt";
    public static final String COMMAND_HELP = "h";
    public static final String COMMAND_INJECT = "i";
    public static final String COMMAND_REQUEST = "r";
    public static final String COMMAND_EXIT = "q";
    public static final String COMMAND_STATUS = "s";
    
    private Hashtable<Integer, ArrayList<ActorRef>> edges;
    private Hashtable<Integer, ActorRef> nodes;
    private final ActorSystem system;
    
    public Manager(String filepath) {
        this.edges = new Hashtable<>();
        this.nodes = new Hashtable<>();
        this.system = ActorSystem.create("ds1project");
        try {
            System.out.println("Building the system with the topology specified in " + filepath);
            BufferedReader inputFile = new BufferedReader(new FileReader(filepath));
            int links = Integer.parseInt(inputFile.readLine());
            System.out.println("Links: " + links);
            for (int i = 0; i < links; i++) {
                String[] link = inputFile.readLine().split(" ");
                int node1id = Integer.parseInt(link[0]);
                int node2id = Integer.parseInt(link[1]);
                System.out.println(node1id + " " + node2id);
                // add the nodes if is the first time they are met
                if (!this.edges.containsKey(node1id)) {
                    this.nodes.put(node1id, this.system.actorOf(Node.props(node1id)));
                    this.edges.put(node1id, new ArrayList<>());
                }
                if (!this.edges.containsKey(node2id)) {
                    this.nodes.put(node2id, this.system.actorOf(Node.props(node2id)));
                    this.edges.put(node2id, new ArrayList<>());
                }
                // adding the connections
                this.edges.get(node1id).add(this.nodes.get(node2id));
                this.edges.get(node2id).add(this.nodes.get(node1id));
            }
            // send the neighbors to each node
            this.nodes.forEach((index, node) -> {
                node.tell(new Startup(this.edges.get(index)), null);
            });
            // starting the interaction with the user
            this.handleCommands();
        } catch (FileNotFoundException ex) {
            System.err.println("Error in opening the topology file. " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("Error in reading the topology file. " + ex.getMessage());
        }
        
    }
    
    public Manager() {
        this(Manager.DEFAULT_FILE);
    }
    
    public void printCommands() {
        System.out.println("List of commands:");
        System.out.println("\t" + Manager.COMMAND_HELP + " -> print the list of commands");
        System.out.println("\t" + Manager.COMMAND_STATUS + " -> print the status of all nodes");
        System.out.println("\t" + Manager.COMMAND_INJECT + " -> inject the token into a node");
        System.out.println("\t" + Manager.COMMAND_REQUEST + " -> request critical section access for a node");
        System.out.println("\t" + Manager.COMMAND_EXIT + " -> quit");
        System.out.println();
    }
    
    public void injectToken(int node) {
        if (node < 0 || node >= this.nodes.size()) {
            System.err.println("Fatal, the node does not exist");
            System.exit(1);
        }
        this.nodes.get(node).tell(new Inject(), null);
    }
    
    public void requestCS(int node) {
        if (node < 0 || node >= this.nodes.size()) {
            System.err.println("Fatal, the node does not exist");
            System.exit(1);
        }
        this.nodes.get(node).tell(new Enter(), null);
    }
    
    public void requestStatus() {
        this.nodes.forEach((index, node) -> {
            node.tell(new Status(), null);
        });
    }
    
    public void handleCommands() {
        boolean goOn = true;
        Scanner in = new Scanner(System.in);
        this.printCommands();
        while(goOn) {
            System.out.println("Type a command:");
            String command = in.next();
            switch(command) {
                case Manager.COMMAND_EXIT:
                    goOn = false;
                    break;
                case Manager.COMMAND_HELP:
                    this.printCommands();
                    break;
                case Manager.COMMAND_INJECT:
                    System.out.println("Select the node to be injected [from 0 to " + (this.nodes.size() - 1) + "]");
                    String node = in.next();
                    this.injectToken(Integer.parseInt(node));
                    break;
                case Manager.COMMAND_REQUEST:
                    System.out.println("Select the node that wants to enter the CS [from 0 to " + (this.nodes.size() - 1) + "]");
                    String n = in.next();
                    this.requestCS(Integer.parseInt(n));
                    break;
                case Manager.COMMAND_STATUS:
                    this.requestStatus();
                    break;
                default: 
                    System.out.println("This command doesn't exist");
            }
        }
    }
}
