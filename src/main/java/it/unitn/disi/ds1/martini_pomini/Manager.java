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
import it.unitn.disi.ds1.martini_pomini.Message.Fail;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

/**
 *
 * @author pomo
 */
public class Manager {
    public static final String DEFAULT_FILE = "src/main/resources/config.json";

    public static final String COMMAND_HELP = "h";
    public static final String COMMAND_INJECT = "i";
    public static final String COMMAND_REQUEST = "r";
    public static final String COMMAND_EXIT = "q";
    public static final String COMMAND_STATUS = "s";
    public static final String COMMAND_FAIL = "f";
    public static final String COMMAND_LOAD = "l";

    private Hashtable<Integer, ArrayList<ActorRef>> edges;
    private Hashtable<Integer, ActorRef> nodes;
    private final ActorSystem system;
    private ConfigParser configManager;
    
    public Manager(String filepath) {
        this.configManager = new ConfigParser(filepath);
        this.edges = new Hashtable<>();
        this.nodes = new Hashtable<>();
        this.system = ActorSystem.create("ds1project");
        System.out.println("Building the system with the topology specified in " + filepath);
        ArrayList<String> links = this.configManager.getNodeTopology();
        for (int i = 0; i < links.size(); i++) {
            String[] link = links.get(i).split(" ");
            int node1id = Integer.parseInt(link[0]);
            int node2id = Integer.parseInt(link[1]);
            System.out.println(node1id + " " + node2id);
            // add the nodes if is the first time they are met
            if (!this.edges.containsKey(node1id)) {
                this.nodes.put(node1id, this.system.actorOf(Node.props(node1id, this.configManager.getMinCSTime(), this.configManager.getMaxCSTime(), this.configManager.getNodeDownTime(), this.configManager.getNodeStartRecoveryTime())));
                this.edges.put(node1id, new ArrayList<>());
            }
            if (!this.edges.containsKey(node2id)) {
                this.nodes.put(node2id, this.system.actorOf(Node.props(node2id, this.configManager.getMinCSTime(), this.configManager.getMaxCSTime(), this.configManager.getNodeDownTime(), this.configManager.getNodeStartRecoveryTime())));
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
    }
    
    public Manager() {
        this(Manager.DEFAULT_FILE);
    }
    
    public void printCommands() {
        System.out.println("List of commands:");
        System.out.println("\t" + Manager.COMMAND_HELP.toUpperCase() + " \u2192 print the list of commands");
        System.out.println("\t" + Manager.COMMAND_STATUS.toUpperCase() + " \u2192 print the status of all nodes");
        System.out.println("\t" + Manager.COMMAND_INJECT.toUpperCase() + " \u2192 inject the token into a node");
        System.out.println("\t" + Manager.COMMAND_REQUEST.toUpperCase() + " \u2192 request critical section access for a node");
        System.out.println("\t" + Manager.COMMAND_FAIL.toUpperCase() + " \u2192 make a node fail and restart");
        System.out.println("\t" + Manager.COMMAND_LOAD.toUpperCase() + " \u2192 specify a path to a .json file, which contains some scripted commands.");
        System.out.println("\t" + Manager.COMMAND_EXIT.toUpperCase() + " \u2192 quit");
        System.out.println();
    }
    
    public void injectToken(int node) {
        if (node < 0 || node >= this.nodes.size()) {
            System.err.println("Fatal, the node does not exist");
            System.exit(1);
        }
        this.nodes.get(node).tell(new Inject(), null);
    }
    
    public void requestCS(int[] nodes) {
        for(int node: nodes) {
            if (node < 0 || node >= this.nodes.size()) {
                System.err.println("Fatal, the node does not exist");
                System.exit(1);
            }
            this.nodes.get(node).tell(new Enter(), null);
        }
    }
    
    public void requestStatus() {
        this.nodes.forEach((index, node) -> {
            node.tell(new Status(), null);
        });
    }
    public void fail(int node) {
        if (node < 0 || node >= this.nodes.size()) {
            System.err.println("Fatal, the node does not exist");
            System.exit(1);
        }
        this.nodes.get(node).tell(new Fail(), null);
    }

    public void load(String path) {
        ConfigParser reader = new ConfigParser(path);
        ArrayList<String> commands = reader.getCommandList();
        System.out.println(commands);
        for (int i = 0; i<commands.size();++i){
            String command = commands.get(i);
            switch(command) {
                case Manager.COMMAND_HELP:
                    this.printCommands();
                    break;
                case Manager.COMMAND_INJECT:
                    this.injectToken(Integer.parseInt(commands.get(++i)));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        System.err.println("Something went wrong with the node init");
                    }
                    break;
                case Manager.COMMAND_REQUEST:
                    int [] n = new int[1];
                    n[0] = Integer.parseInt(commands.get(++i));
                    this.requestCS(n);
                    break;
                case Manager.COMMAND_STATUS:
                    this.requestStatus();
                    break;
                case Manager.COMMAND_FAIL:
                    this.fail(Integer.parseInt(commands.get(++i)));
                    break;
                default:
                    System.out.println("This command doesn't exist or is not allowed in scripted mode");
            }
        }
    }

    
    public void handleCommands() {
        boolean goOn = true;
        Scanner in = new Scanner(System.in);
        this.printCommands();
        String node;
        String file;

        while(goOn) {
            System.out.println("Type a command:");
            String command = in.nextLine().split(" ")[0].toLowerCase();
            switch(command) {
                case Manager.COMMAND_EXIT:
                    goOn = false;
                    System.out.println("Bye");
                    break;
                case Manager.COMMAND_HELP:
                    this.printCommands();
                    break;
                case Manager.COMMAND_INJECT:
                    System.out.println("Select the node to be injected [from 0 to " + (this.nodes.size() - 1) + "]");
                    node = in.nextLine();
                    this.injectToken(Integer.parseInt(node));
                    break;
                case Manager.COMMAND_REQUEST:
                    System.out.println("Select the nodes that wants to enter the CS [from 0 to " + (this.nodes.size() - 1) + "]. Separate them by a space [es. \"0 1 3\"]");
                    String[] nodeList = in.nextLine().split(" ");
                    int [] n = new int[nodeList.length];
                    for (int i = 0; i < nodeList.length; i++) {
                        n[i] = Integer.parseInt(nodeList[i]);
                    }
                    this.requestCS(n);
                    break;
                case Manager.COMMAND_STATUS:
                    this.requestStatus();
                    break;
                case Manager.COMMAND_FAIL:
                    System.out.println("Select the node which should fail [from 0 to " + (this.nodes.size() - 1) + "]");
                    node = in.nextLine();
                    this.fail(Integer.parseInt(node));
                    break;
                case Manager.COMMAND_LOAD:
                    System.out.println("Which file .json do you want to load?");
                    file = in.nextLine();
                    this.load(file);
                    break;
                default: 
                    System.out.println("This command doesn't exist");
            }
        }
        System.exit(0);
    }
}
