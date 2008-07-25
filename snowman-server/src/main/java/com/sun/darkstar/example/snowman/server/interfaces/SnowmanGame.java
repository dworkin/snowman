/*
* Copyright (c) 2008, Sun Microsystems, Inc.
*
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in
*       the documentation and/or other materials provided with the
*       distribution.
*     * Neither the name of Sun Microsystems, Inc. nor the names of its
*       contributors may be used to endorse or promote products derived
*       from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
* "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
* LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
* A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
* OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
* THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
* OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/ 

package com.sun.darkstar.example.snowman.server.interfaces;

import com.sun.sgs.app.ClientSession;
import java.nio.ByteBuffer;

/**
 * The <code>SnowmanGame</code> interface describes the basic behavior
 * for a game
 * 
 * @author Owen Kellett
 */
public interface SnowmanGame 
{
    /**
     * Send a message to all players in the game on the game's Channel
     * @param sess sender of the message
     * @param buff the message itself
     */
    public void send (ClientSession sess, ByteBuffer buff);

    /**
     * Add a player to the game
     * @param player
     * @param color
     */
    public void addPlayer(SnowmanPlayer player, TeamColor color);
    
    public void removePlayer(SnowmanPlayer player);
    
    /**
     * Send the AddMOB packets to all of the players in the game
     * to initiate the game state
     */
    public void sendMapInfo();

    /**
     * Verify that all players are ready to player and start the game 
     * by broadcasting a STARTGAME message if so
     */
    public void startGameIfReady();
    
     /**
     * Respond to an attack message from a player by verifying the attack
     * and broadcasting the result to all other players
     * 
     * @param attacker
     * @param x
     * @param y
     * @param attackedID
     * @param timestamp
     */
    public void attack(SnowmanPlayer attacker, float x, float y, int attackedID,
            long timestamp);

}