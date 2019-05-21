/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unitn.disi.ds1.martini_pomini;

import java.io.Serializable;

/**
 *
 * @author pomo
 */
public class Message {
    // Message to send the token to a node
    public static class Priviledge implements Serializable {}
    
    // Message to request the token
    public static class Request implements Serializable {}
}
