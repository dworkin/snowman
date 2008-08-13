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
package com.sun.darkstar.example.snowman.server.impl;

import com.sun.darkstar.example.snowman.common.physics.enumn.EForce;
import com.sun.darkstar.example.snowman.server.interfaces.SnowmanPlayer;
import com.sun.darkstar.example.snowman.server.interfaces.SnowmanGame;
import com.sun.darkstar.example.snowman.server.interfaces.SnowmanFlag;
import com.sun.darkstar.example.snowman.common.protocol.messages.ServerMessages;
import com.sun.darkstar.example.snowman.common.protocol.processor.IServerProcessor;
import com.sun.darkstar.example.snowman.common.protocol.enumn.ETeamColor;
import com.sun.darkstar.example.snowman.common.util.HPConverter;
import com.sun.darkstar.example.snowman.common.util.Coordinate;
import com.sun.darkstar.example.snowman.common.util.enumn.EStats;
import com.sun.darkstar.example.snowman.server.context.SnowmanAppContext;
import com.sun.darkstar.example.snowman.server.service.GameWorldManager;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.Task;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the player's "proxy" in the world of managed
 * objects.  It implements the ClientSessionListener interface so that
 * it can be the reception point for all client session events.
 * For here, it will call other managed ibjects to respond to thsoe
 * events.
 * @author Jeffrey Kesselman
 * @author Owen Kellett
 */
public class SnowmanPlayerImpl implements SnowmanPlayer, Serializable, 
                                          IServerProcessor
{
    public static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(SnowmanPlayerImpl.class.getName());
    
    static long DEATHDELAYMS = 10 * 1000;
    static float POSITIONTOLERANCESQD = 1.0f;
    static int RESPAWNHP = (int)EStats.SnowmanFullStrength.getValue();
    static int ATTACKHP = (int)EStats.SnowballDamage.getValue();
    
    private ManagedReference<ClientSession> sessionRef;
    
    /**
     * Player information and overall statistics
     */
    private final String name;
    private int wins;
    private int losses;
    
    /**
     * Current game information
     */
    private int id;
    private float startX;
    private float startY;
    private float destX;
    private float destY;
    private long timestamp;
    private ETeamColor teamColor;
    private ManagedReference<SnowmanGame> currentGameRef;
    private ManagedReference<SnowmanFlag> holdingFlagRef;
    private PlayerState state = PlayerState.NONE;
    private int hitPoints = RESPAWNHP;
    private SnowmanAppContext appContext;
    
    public SnowmanPlayerImpl(SnowmanAppContext appContext,
                             ClientSession session) {
        this.appContext = appContext;
        name = session.getName();
        setSession(session);
    } 
    
    static enum PlayerState {
        //indicates the player is not in a game
        NONE, 
        //player is not moving
        STOPPED, 
        //player is moving
        MOVING, 
        //player is dead
        DEAD
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setLocation(float x, float y) {
       startX = destX = x;
       startY = destY = y;
       this.state = PlayerState.STOPPED;
    }

    public void setTeamColor(ETeamColor color) {
        appContext.getDataManager().markForUpdate(this);
        teamColor = color;
    }

    public void setSession(ClientSession arg0) {
        sessionRef = appContext.getDataManager().createReference(arg0);
    }
    
    public void setGame(SnowmanGame game){
        if (game == null){
            currentGameRef = null;
        } else {
            currentGameRef = appContext.getDataManager().createReference(game);
        }
    }
    
    public float ranking(){
        return 1000f*wins/losses;
    }
    
    public float getX() {
        return startX;
    }
    
    public float getY() {
        return startY;
    }
    
    /**
     * This method calculates the projected current position.
     * If the player is not moving, it simply gives the current position as
     * represented by startX and startY.
     * If the player is moving, it calculates the position based on the last
     * known timestamp, the currently given timestamp, the speed (which is
     * calculated according to the current hit points), and the current
     * destination.
     * 
     * @param time
     * @return
     */
    public Coordinate getExpectedPositionAtTime(long time) {
        float currentX = startX;
        float currentY = startY;
        if (state == PlayerState.MOVING) {
            //total time that player has been moving
            long dt = time - timestamp;
            
            //calculate speed from HPconverter
            float ratePerMs = (EForce.Movement.getMagnitude() / HPConverter.getInstance().convertMass(hitPoints)) * 0.00001f;
            float distanceTraveled = ratePerMs*dt;
            
            //calculate the actual distance traveled based on the speed
            float dx = destX - startX;
            float dy = destY - startY;
            float targetDistance = (float)Math.sqrt((dx * dx) + (dy * dy));
            
            //if we've travelled beyond target, target is the position
            if(targetDistance <= distanceTraveled) {
                currentX = destX;
                currentY = destY;
            }
            //otherwise, we need to calculate the new position based on
            //a system of equations (looking for realDX and realDY as variables):
            //  dx/dy = realDX/realDY
            //  realDX^2 + realDY^2 = distanceTraveled^2
            else {
                float realDX = dx == 0.0f ? 0.0f : (float) Math.sqrt(
                        (distanceTraveled * distanceTraveled) /
                        ((dy * dy) / (dx * dx) + 1));
                float realDY = dy == 0.0f ? 0.0f : (float) Math.sqrt(
                        (distanceTraveled * distanceTraveled) / 
                        ((dx*dx)/(dy*dy) + 1));
                
                //ensure that the signs match for the deltas
                if(dx < 0)
                    realDX *= -1;
                if(dy < 0)
                    realDY *= -1;
                
                currentX = startX + realDX;
                currentY = startY + realDY;
            }
        }
        return new Coordinate(currentX, currentY);
    }
    
    /**
     * Check that the given coordinates are within the 
     * given tolerance of eachother
     */
    private boolean checkTolerance(float expectedX, float expectedY,
                                   float givenX, float givenY,
                                   float tolerance) {
        float dx = expectedX - givenX;
        float dy = expectedY - givenY;
        float distanceSqd = (dx*dx)+(dy*dy);
        return distanceSqd < tolerance;
    }
    
    public int getID(){
        return id;
    }
    
    public void setReadyToPlay(boolean readyToPlay){
        if(readyToPlay)
            this.state = PlayerState.STOPPED;
        else
            this.state = PlayerState.NONE;
    }
    
    public boolean getReadyToPlay(){
        return this.state != PlayerState.NONE;
    }
    
    public void send(ByteBuffer buff){
        buff.flip();
        sessionRef.get().send(buff);
    }
    
    public ClientSession getSession(){
        return sessionRef.get();
    }
    
     // IServerProcessor Messages

    public void ready() {
        this.setReadyToPlay(true);
        currentGameRef.get().startGameIfReady();
    }

    public void moveMe(float startx, float starty,
                       float endx, float endy)
    {
        //verify that the start location is valid
        Long now = System.currentTimeMillis();
        moveMe(now, startx, starty, endx, endy);
    }
    protected void moveMe(long now,
                          float startx, float starty,
                          float endx, float endy) {
        //no op if player is dead or not in a game
        if(state == PlayerState.DEAD || state == PlayerState.NONE)
            return;
        
        //verify that the start location is valid
        Coordinate expectedPosition = this.getExpectedPositionAtTime(now);
        
        if (checkTolerance(expectedPosition.getX(), expectedPosition.getY(),
                           startx, starty,
                           POSITIONTOLERANCESQD)) {
            //TODO - collision detection
            Coordinate trimPosition = appContext.getManager(GameWorldManager.class).
                    trimPath(new Coordinate(startx, starty),
                             new Coordinate(endx, endy));
            
            this.timestamp = now;
            this.startX = startx;
            this.startY = starty;
            this.destX = trimPosition.getX();
            this.destY = trimPosition.getY();
            this.state = PlayerState.MOVING;
            
            currentGameRef.get().send(null,
                   ServerMessages.createMoveMOBPkt(
                   id, startX, startY, destX, destY));
        }
        else {
            logger.log(Level.WARNING, "move from {0} failed start position check", name);
            
            this.timestamp = now;
            this.setLocation(expectedPosition.getX(), expectedPosition.getY());
            currentGameRef.get().send(
                    null,
                    ServerMessages.createStopMOBPkt(id, expectedPosition.getX(), expectedPosition.getY()));
        }
    }
    
    public void attack(int targetID, float x, float y) {
        Long now = System.currentTimeMillis();
        attack(now, targetID, x, y);
    }
    protected void attack(long now, int targetID, float x, float y) {
        //no op if player is dead or not in a game
        if(state == PlayerState.DEAD || state == PlayerState.NONE)
            return;
        
        //verify that the start location is valid
        Coordinate expectedPosition = this.getExpectedPositionAtTime(now);
        
        if (checkTolerance(expectedPosition.getX(), expectedPosition.getY(),
                           x, y, POSITIONTOLERANCESQD)) {
            //get the target player and determine its location
            SnowmanPlayer target = currentGameRef.get().getPlayer(targetID);
            Coordinate targetPosition = target.getExpectedPositionAtTime(now);
            
            boolean success = true;
            //verify that target is in range
            float range = HPConverter.getInstance().convertRange(hitPoints);
            if(!checkTolerance(expectedPosition.getX(), expectedPosition.getY(),
                               targetPosition.getX(), targetPosition.getY(), 
                               range*range))
                success = false;
            
            //TODO - collision detection

            //perform implicit stop
            this.timestamp = now;
            this.setLocation(x, y);
            
            if(success) {
                //stop the target
                target.setLocation(targetPosition.getX(), targetPosition.getY());
                currentGameRef.get().send(null,
                                          ServerMessages.createAttackedPkt(
                                          id, targetID, target.hit(ATTACKHP, targetPosition.getX(), targetPosition.getY())));
            }
            else {
                currentGameRef.get().send(null,
                                          ServerMessages.createAttackedPkt(
                                          id, targetID, 0));
            }
        }
        else {
            //ignore an invalid attack
            logger.log(Level.WARNING, "attack from {0} failed attack position check", name);
        }
    }

    public void getFlag(int flagID, float x, float y) {
        Long now = System.currentTimeMillis();
        getFlag(now, flagID, x, y);
    }
    protected void getFlag(long now, int flagID, float x, float y) {
        SnowmanGame game = currentGameRef.get();
        SnowmanFlag flag = game.getFlag(flagID);
        
        // Can not get flag if same team or flag is held by another player
        if (flag == null || 
                flag.getTeamColor() == teamColor || 
                flag.isHeld() ||
                holdingFlagRef != null)
            return;
        
        //verify that the start location is valid
        Coordinate expectedPosition = this.getExpectedPositionAtTime(now);
        if (checkTolerance(expectedPosition.getX(), expectedPosition.getY(),
                           x, y, POSITIONTOLERANCESQD)) {
            
            //verify that the player is in range of the flag
            if(checkTolerance(x, y, flag.getX(), flag.getY(),
                               flag.getGoalRadius()*flag.getGoalRadius())) {
                //perform implicit stop
                this.timestamp = now;
                this.setLocation(x, y);

                //attach the flag
                flag.setHeldBy(this);
                holdingFlagRef = appContext.getDataManager().createReference(flag);
                game.send(null, ServerMessages.createAttachObjPkt(id, flagID));
            }
            else {
                logger.log(Level.WARNING, "get flag from {0} failed radius check", name);
            }
        } 
        else {
            logger.log(Level.WARNING, "get flag from {0} failed attach position check", name);
        }
    }
    
    public void score(float x, float y) {
        
    }
    
    // respawn
    public void respawn() {
        appContext.getDataManager().markForUpdate(this);
        hitPoints = RESPAWNHP;
        Coordinate position = SnowmanMapInfo.getRespawnPosition(SnowmanMapInfo.DEFAULT, this.getTeamColor());
        setLocation(position.getX(), position.getY());
        currentGameRef.get().send(null,
                                  ServerMessages.createRespawnPkt(id, position.getX(), position.getY()));
    }
    
    public int hit(int hp, float attackX, float attackY) {
        if (hitPoints > 0) { // not already dead
            hitPoints -= hp;
            if (hitPoints <= 0) { // newly dead
                // drop flag
                SnowmanFlag flag = holdingFlagRef == null ? null : holdingFlagRef.get();
                if(flag != null) {
                    flag.drop(attackX, attackY);
                }
                holdingFlagRef = null;
                
                // schedule respawn
                appContext.getTaskManager().scheduleTask(
                        new RespawnTask(appContext.getDataManager().createReference((SnowmanPlayer)this)),
                                        DEATHDELAYMS);
            }
        }
        return hp;
    }
    
    public int getHitPoints() {
        return hitPoints;
    }
    
    static private class RespawnTask implements Task, Serializable {
        final ManagedReference<SnowmanPlayer> playerRef;
        
        RespawnTask(ManagedReference<SnowmanPlayer> playerRef) {
            this.playerRef = playerRef;
        }
        
        public void run() throws Exception {
            playerRef.get().respawn();
        }           
    }
    
    public SnowmanGame getGame() {
        return currentGameRef == null ? null : currentGameRef.get();
    }

    public String getName() {
        return name;
    }

    public IServerProcessor getProcessor() {
        return this;
    }

    public ETeamColor getTeamColor() {
        return teamColor;
    }
}