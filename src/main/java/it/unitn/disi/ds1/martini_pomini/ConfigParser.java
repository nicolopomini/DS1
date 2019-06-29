/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unitn.disi.ds1.martini_pomini;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;

/**
 *
 * @author pomo
 */
public class ConfigParser {
    private JsonObject config;
    
    public ConfigParser(String filepath) {
        try {
            String content = new Scanner(new File(filepath)).useDelimiter("\\Z").next();
            System.out.println(content);
            this.config = new JsonParser().parse(content).getAsJsonObject();
        } catch (FileNotFoundException ex) {
            System.err.println("Error in parsing the config file");
            System.exit(1);
        }
    }
    
    public int getMinCSTime() {
        return this.config.getAsJsonPrimitive("min_cs_time").getAsInt();
    }
    
    public int getMaxCSTime() {
        return this.config.getAsJsonPrimitive("max_cs_time").getAsInt();
    }
    
    public int getNodeDownTime() {
        return this.config.getAsJsonPrimitive("node_down_time").getAsInt();
    }
    
    public int getNodeStartRecoveryTime() {
        return this.config.getAsJsonPrimitive("node_start_recovery_time").getAsInt();
    }
    
    public ArrayList<String> getNodeTopology() {
        ArrayList<String> topology = new ArrayList<>();
        JsonArray links = this.config.getAsJsonArray("topology");
        for (JsonElement link: links) {
            JsonObject linkObj = link.getAsJsonObject();
            topology.add(linkObj.getAsJsonPrimitive("source").getAsInt() + " " + linkObj.getAsJsonPrimitive("dest").getAsInt());
        }
        return topology;
    }
    
    public ArrayList<String> getCommandList() {
        ArrayList<String> commands = new ArrayList<>();
        JsonArray cmd = this.config.getAsJsonArray("commands");
        for (JsonElement command: cmd) {
            JsonObject cmdObj = command.getAsJsonObject();
            commands.add(cmdObj.getAsJsonPrimitive("command").getAsString());
            if (cmdObj.has("arg"))
                commands.add(cmdObj.getAsJsonPrimitive("arg").getAsString());
        }
        return commands;
    }
}
